import http from 'k6/http';
import {check, sleep} from 'k6';
import {Rate, Trend} from 'k6/metrics';

// -------- 이미지 업로드 파이프라인 부하테스트 --------
// 실행 모드 (SCENARIO env):
//   ingest: POST throughput 측정 (RPS 10→200, Worker ON)
//   e2e:    end-to-end async latency 측정 (3 POST/s + /status polling)
//
// 기본값: 50 users × 3 notes = 150 note pool
// 요청마다 (user, note) 랜덤 선택 → DB 캐시 편향 방지
//
// 실행:
//   1) ingest (처리량)
//      k6 run perf/image_upload_throughput.js \
//        -e SCENARIO=ingest \
//        -e BASE_URL=https://api.keepgoingapp.com \
//        -e USER_COUNT=50 \
//        -e TEST_PASSWORD='Test1234!' \
//        --summary-export perf/results/ingest.json
//
//   2) PEL=0 확인 (Worker drain 대기)
//      until redis-cli XPENDING note-image-processing-requests keepgoing-worker | awk '{print $1}' | grep -q '^0$'; do sleep 5; done
//
//   3) e2e (종단간 지연)
//      k6 run perf/image_upload_throughput.js \
//        -e SCENARIO=e2e \
//        -e BASE_URL=https://api.keepgoingapp.com \
//        -e USER_COUNT=50 \
//        -e TEST_PASSWORD='Test1234!' \
//        --summary-export perf/results/e2e.json
//
// 정리:
//   teardown()에서 생성된 note + note_images 자동 soft-delete
//   R2 오브젝트는 Lifecycle Policy(1일)로 자동 정리
// ================================================

// ---------- 커스텀 메트릭 ----------
const uploadDuration = new Trend('upload_duration');
const uploadSuccess = new Rate('upload_success');
const smallDuration = new Trend('upload_small_duration');
const mediumDuration = new Trend('upload_medium_duration');
const largeDuration = new Trend('upload_large_duration');

const processLatency = new Trend('process_latency');
const processSuccess = new Rate('process_success');
const pollingCount = new Trend('polling_count');

// ---------- Init Context ----------
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCENARIO = __ENV.SCENARIO || 'ingest';
const USER_COUNT = parseInt(__ENV.USER_COUNT || '50');
const NOTE_COUNT_PER_USER = parseInt(__ENV.NOTE_COUNT_PER_USER || '3');
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'Test1234!';
const EMAIL_PREFIX = 'k6-upload-test';

const IMAGE_FILES = [
    {name: 'small.jpg', path: './assets/small.jpg', tag: 'small'},
    {name: 'medium.jpg', path: './assets/medium.jpg', tag: 'medium'},
    {name: 'large.jpg', path: './assets/large.jpg', tag: 'large'},
];
const IMAGE_BYTES = IMAGE_FILES.map(f => ({...f, data: open(f.path, 'b')}));

function pickImage() {
    return IMAGE_BYTES[Math.floor(Math.random() * IMAGE_BYTES.length)];
}

function safeJson(r) {
    try {return r.json();} catch {return null;}
}

function pickAccount(accounts) {
    return accounts[Math.floor(Math.random() * accounts.length)];
}

// ---------- 부하 프로필 ----------
const createIngestScenarios = () => ({
    warmup: {
        executor: 'constant-arrival-rate',
        rate: 3, timeUnit: '1s', duration: '20s',
        preAllocatedVUs: 5, maxVUs: 20, exec: 'warmup',
    },
    ingest: {
        executor: 'ramping-arrival-rate',
        startRate: 10, timeUnit: '1s',
        stages: [
            {duration: '30s', target: 30},
            {duration: '30s', target: 60},
            {duration: '30s', target: 120},
            {duration: '30s', target: 200},
            {duration: '30s', target: 0},
        ],
        preAllocatedVUs: 20, maxVUs: 200,
        exec: 'ingest', startTime: '20s',
    },
});

const createE2eScenarios = () => ({
    warmup: {
        executor: 'constant-arrival-rate',
        rate: 1, timeUnit: '1s', duration: '20s',
        preAllocatedVUs: 3, maxVUs: 10, exec: 'warmup',
    },
    e2e: {
        executor: 'constant-arrival-rate',
        rate: 3, timeUnit: '1s', duration: '2m',
        preAllocatedVUs: 5, maxVUs: 30,
        exec: 'e2e', startTime: '20s',
    },
});

const scenario = SCENARIO === 'e2e' ? createE2eScenarios() : createIngestScenarios();

export const options = {
    scenarios: scenario,
    thresholds: SCENARIO === 'e2e'
        ? {
            'process_latency':       ['p(95)<30000'],
            'process_success':       ['rate>0.95'],
        }
        : {
            'http_req_duration{scenario:ingest}': ['p(95)<5000'],
            'http_req_failed{scenario:ingest}':   ['rate<0.01'],
            'upload_small_duration':               ['p(95)<3000'],
            'upload_medium_duration':              ['p(95)<4000'],
            'upload_large_duration':               ['p(95)<6000'],
        },
};

// ---------- Setup: signup → login → note 생성 ----------
export function setup() {
    const accounts = [];

    for (let i = 1; i <= USER_COUNT; i++) {
        const email = `${EMAIL_PREFIX}-${i}@test.com`;

        http.post(`${BASE_URL}/api/auth/signup`, JSON.stringify({
            email,
            password: TEST_PASSWORD,
            name: `${EMAIL_PREFIX}-${i}`,
        }), {headers: {'Content-Type': 'application/json'}});

        const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
            email,
            password: TEST_PASSWORD,
        }), {headers: {'Content-Type': 'application/json'}});

        if (loginRes.status !== 200) {
            console.warn(`[${i}] login failed: ${loginRes.status}`);
            continue;
        }

        const rawCookie = loginRes.headers['Set-Cookie']
            || loginRes.headers['set-cookie']
            || '';
        const values = Array.isArray(rawCookie) ? rawCookie : [rawCookie];
        let token = null;
        for (const entry of values) {
            const m = String(entry).match(/access_token=([^;]+)/);
            if (m) { token = m[1]; break; }
        }
        if (!token) {
            console.warn(`[${i}] token not found in Set-Cookie`);
            continue;
        }

        const authHeader = {Authorization: `Bearer ${token}`};
        const createdNoteIds = [];

        for (let n = 1; n <= NOTE_COUNT_PER_USER; n++) {
            const noteRes = http.post(`${BASE_URL}/api/notes`, JSON.stringify({
                title: `${EMAIL_PREFIX}-${i}-note-${n}`,
                content: 'created by k6 setup',
                visibility: 'PRIVATE',
            }), {headers: {...authHeader, 'Content-Type': 'application/json'}});

            if (noteRes.status !== 201) {
                console.warn(`[${i}] note ${n} creation failed: ${noteRes.status}`);
                continue;
            }

            const noteId = noteRes.json().data?.noteId;
            if (noteId) createdNoteIds.push(noteId);
        }

        for (const noteId of createdNoteIds) {
            accounts.push({token, noteId, email});
        }
    }

    if (accounts.length === 0) {
        throw new Error('No accounts could be prepared');
    }
    console.log(`Setup done: ${accounts.length}/${USER_COUNT} accounts (mode: ${SCENARIO})`);
    return {accounts};
}

// ---------- Teardown: 생성된 노트 정리 ----------
export function teardown(data) {
    if (!data || !data.accounts || data.accounts.length === 0) return;

    let deleted = 0;
    for (const acct of data.accounts) {
        const res = http.del(
            `${BASE_URL}/api/notes/${acct.noteId}`,
            null,
            {headers: {Authorization: `Bearer ${acct.token}`}},
        );
        if (res.status === 204) {
            deleted++;
        } else {
            console.warn(`Teardown: note ${acct.noteId} failed: ${res.status} ${res.body}`);
        }
    }
    console.log(`Teardown: ${deleted}/${data.accounts.length} notes deleted`);
}

// ---------- Exec: warmup (POST only) ----------
export function warmup(data) {
    const acct = pickAccount(data.accounts);
    const image = pickImage();

    http.post(
        `${BASE_URL}/api/notes/${acct.noteId}/images`,
        {file: http.file(image.data, image.name, 'image/jpeg')},
        {headers: {Authorization: `Bearer ${acct.token}`},
         tags: {phase: 'warmup', endpoint: 'upload'}},
    );
}

// ---------- Exec: ingest (POST only, RPS ramp) ----------
export function ingest(data) {
    const acct = pickAccount(data.accounts);
    const image = pickImage();

    const res = http.post(
        `${BASE_URL}/api/notes/${acct.noteId}/images`,
        {file: http.file(image.data, image.name, 'image/jpeg')},
        {headers: {Authorization: `Bearer ${acct.token}`},
         tags: {scenario: 'ingest', size: image.tag, endpoint: 'upload'}},
    );

    const body = safeJson(res);
    const passed = check(res, {
        'status is 201': r => r.status === 201,
        'success=true': r => body && body.success === true,
        'has publicId': r => body && typeof body.data?.publicId === 'string',
    }, {scenario: 'ingest', size: image.tag});

    uploadDuration.add(res.timings.duration, {scenario: 'ingest', size: image.tag});
    uploadSuccess.add(passed);

    if (image.tag === 'small') smallDuration.add(res.timings.duration);
    if (image.tag === 'medium') mediumDuration.add(res.timings.duration);
    if (image.tag === 'large') largeDuration.add(res.timings.duration);
}

// ---------- Exec: e2e (POST + /status polling, 저RPS) ----------
export function e2e(data) {
    const acct = pickAccount(data.accounts);
    const image = pickImage();

    // 1) 업로드
    const uploadRes = http.post(
        `${BASE_URL}/api/notes/${acct.noteId}/images`,
        {file: http.file(image.data, image.name, 'image/jpeg')},
        {headers: {Authorization: `Bearer ${acct.token}`},
         tags: {scenario: 'e2e', endpoint: 'upload'}},
    );

    const uploadBody = safeJson(uploadRes);
    if (!uploadBody || !uploadBody.data || !uploadBody.data.publicId) {
        processSuccess.add(false);
        return;
    }

    const publicId = uploadBody.data.publicId;
    const startTime = Date.now();
    let polled = 0;
    let completed = false;

    // 2) /status polling (max 30회, 1s 간격)
    for (let i = 0; i < 30; i++) {
        sleep(1);
        polled++;

        const statusRes = http.get(
            `${BASE_URL}/api/notes/${acct.noteId}/images/${publicId}/status`,
            {headers: {Authorization: `Bearer ${acct.token}`},
             tags: {scenario: 'e2e', endpoint: 'status'}},
        );

        const statusBody = safeJson(statusRes);
        if (!statusBody || !statusBody.data) continue;

        const status = statusBody.data.status;
        if (status === 'SAFE') {
            processLatency.add(Date.now() - startTime);
            pollingCount.add(polled);
            processSuccess.add(true);
            completed = true;
            break;
        }
        if (status === 'REJECTED') {
            processLatency.add(Date.now() - startTime);
            pollingCount.add(polled);
            processSuccess.add(false);
            completed = true;
            break;
        }
    }

    if (!completed) {
        processSuccess.add(false);
        pollingCount.add(polled);
    }
}
