#!/usr/bin/env bash
# =====================================================================
# Run the full ecommerce platform locally.
#   1. docker compose up -d          (6 × postgres + redis + kafka KRaft)
#   2. mvn spring-boot:run for each service on the host JVM
#
# Each service writes to logs/<service>.log. Press Ctrl-C to stop
# everything (services + containers).
# =====================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

mkdir -p logs
PIDS=()
# Boot order matters: eureka first, auth+catalog before cart/order, gateway last.
BOOT_ORDER=(eureka-server auth-service user-profile-service catalog-service inventory-service cart-service shipping-service payment-service notification-service search-service analytics-service flash-sale-service voucher-service order-service api-gateway)

cleanup() {
    echo ""
    echo "🛑  Shutting down services..."
    for pid in "${PIDS[@]:-}"; do
        [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null && kill "$pid" 2>/dev/null || true
    done
    sleep 2
    for pid in "${PIDS[@]:-}"; do
        kill -9 "$pid" 2>/dev/null || true
    done
    echo "🛑  Stopping docker infra..."
    docker compose stop >/dev/null 2>&1 || true
    echo "✅  Done."
}
trap cleanup INT TERM EXIT

echo "🐳  Starting postgres + redis + kafka..."
docker compose up -d

echo "⏳  Waiting for infra to be healthy..."
# postgres (per-service) + redis = healthy containers; kafka reports healthy after ~30s.
for _ in {1..60}; do
    healthy=$(docker compose ps --format '{{.Health}}' | grep -c healthy || true)
    [[ "$healthy" -ge 9 ]] && break
    sleep 2
done

start_service() {
    local name="$1"
    local logfile="logs/${name}.log"
    [[ -d "$name" ]] || { echo "⏭️   ${name} not yet implemented — skipping"; return; }
    echo "🚀  Starting ${name} (logs → ${logfile})"
    ( cd "$name" && mvn -q spring-boot:run > "../${logfile}" 2>&1 ) &
    PIDS+=($!)
}

for svc in "${BOOT_ORDER[@]}"; do
    start_service "$svc"
    # eureka needs a head start; other services can overlap.
    [[ "$svc" == "eureka-server" ]] && sleep 10 || sleep 5
done

echo ""
echo "✅  Platform launching. Key endpoints:"
echo "    eureka        http://localhost:8761     (admin / admin123)"
echo "    api-gateway   http://localhost:8080"
echo "    auth-service  http://localhost:8081     gRPC :9091"
echo "    catalog       http://localhost:8083     gRPC :9093"
echo "    cart          http://localhost:8084     gRPC :9094"
echo "    order         http://localhost:8085     gRPC :9095"
echo "    payment       http://localhost:8086"
echo "    shipping      http://localhost:8087"
echo ""
echo "📝  Tail:  tail -f logs/*.log"
echo "🛑  Ctrl-C to stop everything."
echo ""

# Stream every running service's log.
tail -f logs/*.log
