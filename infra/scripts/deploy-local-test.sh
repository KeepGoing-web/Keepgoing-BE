#!/usr/bin/env bash
# deploy-local-test.sh - Build JARs locally and deploy to test server
# Usage: ./infra/scripts/deploy-local-test.sh <user@host>
set -euo pipefail

SSH_TARGET="${1:?Usage: deploy-local-test.sh <user@host>}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "=== 1. Building JARs ==="
cd "$ROOT_DIR"
./gradlew :keepgoing-api:bootJar :keepgoing-worker:bootJar --no-daemon

echo "=== 2. Copying JARs to staging ==="
mkdir -p build/test-deploy
cp keepgoing-api/build/libs/keepgoing-api-*.jar build/test-deploy/api.jar
cp keepgoing-worker/build/libs/keepgoing-worker-*.jar build/test-deploy/worker.jar

echo "=== 3. Copying JARs to server ==="
ssh "$SSH_TARGET" "mkdir -p /srv/keepgoing-test/build"
scp build/test-deploy/api.jar "$SSH_TARGET:/srv/keepgoing-test/build/api.jar"
scp build/test-deploy/worker.jar "$SSH_TARGET:/srv/keepgoing-test/build/worker.jar"

echo "=== 4. Copying local override compose file ==="
scp "$ROOT_DIR/infra/docker-compose.test.yml" "$SSH_TARGET:/srv/keepgoing-test/docker-compose.test.yml"
scp "$ROOT_DIR/infra/docker-compose.test.local.yml" "$SSH_TARGET:/srv/keepgoing-test/docker-compose.test.local.yml"

echo "=== 5. Deploying on server ==="
ssh "$SSH_TARGET" "cd /srv/keepgoing-test && \
  docker compose --env-file .env.test -f docker-compose.test.yml -f docker-compose.test.local.yml up -d --pull never"

echo ""
echo "=== Done! ==="
echo "Connect: ssh -L 8082:localhost:8082 $SSH_TARGET"
echo "Test:    curl http://localhost:8082/actuator/health"
echo ""
echo "=== k6 Load Test ==="
echo "Option A (로컬 k6, SSH 터널, 권장):"
echo "  # Terminal 1: SSH tunnel"
echo "  ssh -L 8082:localhost:8082 $SSH_TARGET"
echo "  # Terminal 2: k6 from local machine"
echo "  k6 run perf/image_upload_throughput.js -e BASE_URL=http://localhost:8082"
echo ""
echo "Option B (서버 Docker k6, CPU 경합 주의):"
echo "  scp -r perf $SSH_TARGET:/srv/keepgoing-test/"
echo "  ssh $SSH_TARGET 'docker run --rm --network keepgoing-test_keepgoing-test-net"
echo "    -v /srv/keepgoing-test/perf:/scripts -w /scripts grafana/k6"
echo "    run image_upload_throughput.js -e BASE_URL=http://test-app:8080"
echo "    --out output-prometheus-remote=http://prometheus:9090/api/v1/write'"
echo ""
echo "Option C (로컬 k6 + Prometheus):"
echo "  위 Option A로 테스트 실행 후, 서버에서 Docker k6로 Prometheus 전송만 수행"
