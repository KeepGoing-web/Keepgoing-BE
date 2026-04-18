import http from "k6/http";
import { check, sleep } from "k6";

// warmup 10s -> measure 30s
export const options = {
    scenarios: {
        warmup: {
            executor: "constant-vus",
            vus: 10,
            duration: "10s",
            exec: "warmup",
            gracefulStop: "0s",
        },
        measure: {
            executor: "constant-vus",
            vus: 10,
            duration: "30s",
            exec: "measure",
            startTime: "10s",
            gracefulStop: "0s",
        },
    },
    thresholds: {
        "http_req_failed{phase:measure}": ["rate<0.01"],
        "http_req_duration{phase:measure}": ["p(95)<2000"],
        "http_reqs{phase:measure}": ["count>0"],
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const COOKIE_HEADER = __ENV.COOKIE_HEADER;
const KEYWORD = __ENV.KEYWORD || "spring";

function requestOnce(phase) {
    if (!COOKIE_HEADER) {
        throw new Error(
            'COOKIE_HEADER env is required. Example: COOKIE_HEADER="access_token=..."'
        );
    }

    const qs =
        `keyword=${encodeURIComponent(KEYWORD)}` +
        `&page=0&size=10`;

    const url = `${BASE_URL}/api/notes/me/search-slice?${qs}`;

    const res = http.get(url, {
        headers: {
            Cookie: COOKIE_HEADER,
        },
        tags: { phase, variant: "LIKE", response: "SLICE", db: "POSTGRES" },
    });

    check(
        res,
        {
            "status is 200": (r) => r.status === 200,
            "success=true": (r) => {
                try {
                    return r.json().success === true;
                } catch {
                    return false;
                }
            },
            "contents length > 0": (r) => {
                try {
                    const body = r.json();
                    const data = body.data || {};
                    const contents = data.contents || [];
                    return contents.length > 0;
                } catch {
                    return false;
                }
            },
            "hasNext exists": (r) => {
                try {
                    const body = r.json();
                    const data = body.data || {};
                    return typeof data.hasNext === "boolean";
                } catch {
                    return false;
                }
            },
        },
        { phase }
    );

    sleep(1);
}

export function warmup() {
    requestOnce("warmup");
}

export function measure() {
    requestOnce("measure");
}
