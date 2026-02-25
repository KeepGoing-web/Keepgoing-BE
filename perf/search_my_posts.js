import http from "k6/http";
import { check, sleep } from "k6";

//warmup 10s → measure 30s
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
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const TOKEN = __ENV.TOKEN;
const KEYWORD = __ENV.KEYWORD || "spring";

function requestOnce(phase) {
    const url = `${BASE_URL}/api/posts/me/search?keyword=${encodeURIComponent(
        KEYWORD
    )}&page=0&size=10`;

    const res = http.get(url, {
        headers: { Authorization: `Bearer ${TOKEN}` },
        tags: { phase },
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
        },
        { phase }
    );

    sleep(1);
}

// warm-up 단계
export function warmup() {
    requestOnce("warmup");
}

// 측정 단계
export function measure() {
    requestOnce("measure");
}