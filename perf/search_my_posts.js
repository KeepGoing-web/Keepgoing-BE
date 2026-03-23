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
const VARIANT = (__ENV.VARIANT || "LIKE").toUpperCase(); // LIKE | FULLTEXT
const MODE = (__ENV.MODE || "SCORE").toUpperCase(); // SCORE | NEWEST (only for FULLTEXT)

function requestOnce(phase) {
    if (!TOKEN) {
        throw new Error("TOKEN env is required. Example: TOKEN=\"<ACCESS_TOKEN>\"");
    }

    let path;
    if (VARIANT === "LIKE") {
        path = "/api/posts/me/search-like";
    } else {
        // FULLTEXT
        path = "/api/posts/me/search";
    }

    const qs =
        `keyword=${encodeURIComponent(KEYWORD)}` +
        `&page=0&size=10` +
        (VARIANT !== "LIKE" ? `&mode=${encodeURIComponent(MODE)}` : "");

    const url = `${BASE_URL}${path}?${qs}`;
    const res = http.get(url, {
        headers: { Authorization: `Bearer ${TOKEN}` },
        tags: { phase, variant: VARIANT, mode: VARIANT === "LIKE" ? "-" : MODE },
    });

    check(
        res,
        {
            [`status is 200 (${VARIANT}${VARIANT === "LIKE" ? "" : "/" + MODE})`]: (r) => r.status === 200,
            [`success=true (${VARIANT}${VARIANT === "LIKE" ? "" : "/" + MODE})`]: (r) => {
                try {
                    return r.json().success === true;
                } catch {
                    return false;
                }
            },
            [`totalElements > 0 (${VARIANT}${VARIANT === "LIKE" ? "" : "/" + MODE})`]: (r) => {
                try {
                    const body = r.json();
                    const data = body.data || {};
                    return (data.totalElements || 0) > 0;
                } catch {
                    return false;
                }
            },
            [`contents length > 0 (${VARIANT}${VARIANT === "LIKE" ? "" : "/" + MODE})`]: (r) => {
                try {
                    const body = r.json();
                    const data = body.data || {};
                    const contents = data.contents || [];
                    return contents.length > 0;
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