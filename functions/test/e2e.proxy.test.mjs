import { test } from 'node:test';
import assert from 'node:assert';

// ---------------------------------------------------------------------------
// End-to-end integration test for the deployed Gemini proxy Cloud Function.
//
// Uses only Node 20 built-ins: the node:test runner and global fetch.
// No external npm dependencies.
//
// Env:
//   PROXY_URL             - deployed proxy endpoint (has a sane default)
//   FIREBASE_WEB_API_KEY  - required; used to mint an anonymous Firebase ID token
//   EXPECT_MODEL          - optional; if set, assert body.model === EXPECT_MODEL
// ---------------------------------------------------------------------------

const PROXY_URL =
  process.env.PROXY_URL ||
  'https://us-central1-kinetic-ai-coach-50627.cloudfunctions.net/geminiProxy';

const FIREBASE_WEB_API_KEY = process.env.FIREBASE_WEB_API_KEY;
const EXPECT_MODEL = process.env.EXPECT_MODEL;

const FETCH_TIMEOUT_MS = 30_000;

// fetch wrapper with an AbortController-based timeout so a hung request
// doesn't stall CI indefinitely.
async function fetchWithTimeout(url, options = {}, timeoutMs = FETCH_TIMEOUT_MS) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

// Mint an anonymous Firebase ID token via the Identity Toolkit REST API.
async function mintAnonIdToken(apiKey) {
  const url = `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${apiKey}`;
  const res = await fetchWithTimeout(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ returnSecureToken: true }),
  });
  const text = await res.text();
  assert.strictEqual(
    res.status,
    200,
    `identitytoolkit signUp failed: HTTP ${res.status} - ${text}`
  );
  let json;
  try {
    json = JSON.parse(text);
  } catch (e) {
    throw new Error(`identitytoolkit returned non-JSON: ${text}`);
  }
  assert.ok(
    typeof json.idToken === 'string' && json.idToken.length > 0,
    `identitytoolkit response missing idToken: ${text}`
  );
  return json.idToken;
}

test('proxy rejects unauthenticated requests', async () => {
  const res = await fetchWithTimeout(PROXY_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt: 'ping' }),
  });
  assert.strictEqual(
    res.status,
    401,
    `expected 401 for unauthenticated request, got ${res.status}`
  );
});

test('proxy answers an authenticated prompt end-to-end', async () => {
  if (!FIREBASE_WEB_API_KEY) {
    throw new Error(
      'FIREBASE_WEB_API_KEY env var is required to run the authenticated E2E test'
    );
  }

  const idToken = await mintAnonIdToken(FIREBASE_WEB_API_KEY);

  const res = await fetchWithTimeout(PROXY_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({ prompt: 'Reply with exactly one word: PONG' }),
  });

  const text = await res.text();
  assert.strictEqual(
    res.status,
    200,
    `expected 200 for authenticated request, got ${res.status} - ${text}`
  );

  let body;
  try {
    body = JSON.parse(text);
  } catch (e) {
    throw new Error(`proxy returned non-JSON body: ${text}`);
  }

  assert.ok(
    typeof body.text === 'string' && body.text.trim().length > 0,
    `expected non-empty body.text, got: ${JSON.stringify(body.text)}`
  );
  assert.ok(
    typeof body.model === 'string' && body.model.startsWith('gemini'),
    `expected body.model to start with "gemini", got: ${JSON.stringify(body.model)}`
  );

  if (EXPECT_MODEL) {
    assert.strictEqual(
      body.model,
      EXPECT_MODEL,
      `expected model ${EXPECT_MODEL}, got ${body.model}`
    );
  }
});
