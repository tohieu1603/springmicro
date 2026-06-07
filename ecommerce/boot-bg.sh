#!/usr/bin/env bash
# Boot all services as detached background processes so they survive shell exit.
# Each service writes pid to logs/<svc>.pid + log to logs/<svc>.log.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
mkdir -p logs

# Shared dev secrets — every Spring service needs the same JWT secret
# (HS256 ≥ 32 chars) so tokens issued by auth-service validate everywhere.
# Eureka uses localhost hostname so the gateway routes survive WiFi reconnects.
export JWT_SECRET="${JWT_SECRET:-luxury-mart-dev-jwt-shared-secret-2026-at-least-32-chars-long-please}"
export EUREKA_INSTANCE_HOSTNAME="${EUREKA_INSTANCE_HOSTNAME:-localhost}"
export EUREKA_INSTANCE_PREFER_IP_ADDRESS="${EUREKA_INSTANCE_PREFER_IP_ADDRESS:-false}"

BOOT_ORDER=(eureka-server auth-service user-profile-service catalog-service inventory-service cart-service shipping-service payment-service notification-service search-service analytics-service flash-sale-service voucher-service order-service api-gateway)

start() {
  local svc="$1"
  [[ -d "$svc" ]] || { echo "⏭️   $svc — skip"; return; }
  if [[ -f "logs/$svc.pid" ]] && kill -0 "$(cat "logs/$svc.pid")" 2>/dev/null; then
    echo "⚠️   $svc already running (pid $(cat "logs/$svc.pid"))"
    return
  fi
  echo "🚀  $svc"
  nohup bash -c "cd '$ROOT/$svc' && JWT_SECRET='$JWT_SECRET' EUREKA_INSTANCE_HOSTNAME='$EUREKA_INSTANCE_HOSTNAME' EUREKA_INSTANCE_PREFER_IP_ADDRESS='$EUREKA_INSTANCE_PREFER_IP_ADDRESS' mvn -q spring-boot:run" > "logs/$svc.log" 2>&1 &
  echo $! > "logs/$svc.pid"
  disown
}

for svc in "${BOOT_ORDER[@]}"; do
  start "$svc"
  # Eureka needs head start.
  if [[ "$svc" == "eureka-server" ]]; then sleep 25; else sleep 4; fi
done
echo "✅  All services launched in background. Tail logs/<svc>.log for output."
