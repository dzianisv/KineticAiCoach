#!/usr/bin/env bash
set -euo pipefail

# Creates a Firebase Web App and GA4 data stream for the Kinetic landing page,
# then prints the measurement ID so you can set it in .env or CI secrets.
#
# Prerequisites:
#   1. firebase-tools installed (npm install -g firebase-tools)
#   2. Logged in:  firebase login
#   3. Project set: firebase use kinetic-ai-coach-50627
#
# Usage:
#   ./scripts/setup-web-analytics.sh

PROJECT_ID="kinetic-ai-coach-50627"
APP_NAME="kinetic-landing-page"

echo "==> Checking Firebase login..."
firebase projects:list --json 2>/dev/null | grep -q "$PROJECT_ID" || {
  echo "Not logged in or project not accessible. Run:  firebase login"
  exit 1
}

echo "==> Checking for existing Web apps..."
EXISTING=$(firebase apps:list WEB --json 2>/dev/null)
APP_ID=$(echo "$EXISTING" | python3 -c "
import sys,json
try:
    apps = json.load(sys.stdin).get('apps', [])
    for a in apps:
        if '$APP_NAME' in a.get('displayName','').lower():
            print(a['appId'])
except: pass
")

if [ -n "$APP_ID" ]; then
  echo "Web app already exists (appId=$APP_ID). Fetching config..."
else
  echo "==> Creating new Firebase Web app '$APP_NAME'..."
  RESULT=$(echo y | firebase apps:create WEB "$APP_NAME" --json 2>/dev/null)
  APP_ID=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('appId',''))")
  echo "Created appId=$APP_ID"
fi

echo "==> Fetching Web app config (includes measurementId)..."
CONFIG=$(curl -s -H "Authorization: Bearer $(gcloud auth print-access-token 2>/dev/null)" \
  "https://firebase.googleapis.com/v1beta1/projects/$PROJECT_ID/webApps/$APP_ID/config" 2>/dev/null)

MEASUREMENT_ID=$(echo "$CONFIG" | python3 -c "
import sys,json
try:
    print(json.load(sys.stdin).get('measurementId', 'NOT_FOUND'))
except: print('NOT_FOUND')
")

echo ""
echo "=== GA4 Measurement ID: $MEASUREMENT_ID ==="
echo ""
echo "Set it in your environment:"
echo "  export GA_MEASUREMENT_ID=$MEASUREMENT_ID"
echo "  echo \"GA_MEASUREMENT_ID=$MEASUREMENT_ID\" >> .env"
echo ""
echo "Then update docs/index.html: replace G-MEASUREMENT_ID with $MEASUREMENT_ID"
echo "Or add GA_MEASUREMENT_ID to GitHub Actions secrets for deploy-time replacement."
