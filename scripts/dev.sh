#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -f .env ]; then set -a; source .env; set +a; fi
mkdir -p .runtime
for port in 8080 8081 5173; do
  if lsof -tiTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then echo "Porta $port ocupada. Encerre a instância anterior antes de iniciar."; exit 1; fi
done
./mvnw -q -DskipTests package
if [ ! -d frontend/node_modules ]; then npm ci --prefix frontend; fi
pids=()
cleanup(){ for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done; }
trap cleanup EXIT INT TERM
java -jar bilheteria/target/codeflex-bilheteria-0.0.1-SNAPSHOT.jar > .runtime/bilheteria.log 2>&1 & pids+=("$!")
java -jar pagamentos/target/codeplax-pagamentos-api-0.0.1-SNAPSHOT.jar > .runtime/pagamentos.log 2>&1 & pids+=("$!")
(cd frontend && exec node node_modules/vite/bin/vite.js --host 127.0.0.1) > .runtime/frontend.log 2>&1 & pids+=("$!")
echo 'Codeflex iniciando: http://localhost:5173 | Logs em .runtime/'
wait
