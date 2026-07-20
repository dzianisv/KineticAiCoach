#!/usr/bin/env bash
# Run a CUA (computer-use) e2e case against a running Android emulator.
# Usage: e2e/cua/run.sh [CASE] [SERIAL] [OUTPUT_DIR]
#   CASE       path to a .yaml case (default: e2e/cua/full_flow.yaml)
#   SERIAL     adb serial of the target emulator (default: emulator-5562)
#   OUTPUT_DIR where screenshots/gif/video/result.json land (default: /tmp/cua-<case>)
#
# Requires the a-test harness checked out (default ../a-test, override A_TEST_DIR)
# and its .venv created. Backend = Azure Dev AI, model gpt-5.4 (gpt-4o returns 404).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CASE="${1:-$REPO_ROOT/e2e/cua/full_flow.yaml}"
SERIAL="${2:-emulator-5562}"
CASE_NAME="$(basename "${CASE%.*}")"
OUTPUT_DIR="${3:-/tmp/cua-$CASE_NAME}"
A_TEST_DIR="${A_TEST_DIR:-$(cd "$REPO_ROOT/.." && pwd)/a-test}"

[ -d "$A_TEST_DIR" ] || { echo "a-test not found at $A_TEST_DIR (set A_TEST_DIR)"; exit 2; }
[ -f "$A_TEST_DIR/.venv/bin/python" ] || { echo "a-test venv missing at $A_TEST_DIR/.venv"; exit 2; }

# Vision backend creds: prefer already-exported AZURE_CUA_*, else derive from Azure Dev AI.
if [ -z "${AZURE_CUA_API_KEY:-}" ]; then
  # shellcheck disable=SC1090
  [ -f "$HOME/.env.d/azure-dev.env" ] && source "$HOME/.env.d/azure-dev.env"
  export AZURE_CUA_API_KEY="${AZURE_DEV_AI_API_KEY:?set AZURE_DEV_AI_API_KEY or AZURE_CUA_API_KEY}"
  export AZURE_CUA_BASE_URL="${AZURE_DEV_AI_BASE_URL:?set AZURE_DEV_AI_BASE_URL or AZURE_CUA_BASE_URL}"
fi
export AZURE_CUA_MODEL="${AZURE_CUA_MODEL:-gpt-5.4}"

# a_test/android.py calls bare `adb`; pin the target device when >1 is attached.
export ANDROID_SERIAL="$SERIAL"

echo "case=$CASE serial=$SERIAL model=$AZURE_CUA_MODEL out=$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"
cd "$A_TEST_DIR"
exec ./.venv/bin/python -m a_test run \
  --target android \
  --case "$CASE" \
  --model "$AZURE_CUA_MODEL" \
  --output-dir "$OUTPUT_DIR"
