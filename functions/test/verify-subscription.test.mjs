import { test } from 'node:test';
import assert from 'node:assert';
import { createRequire } from 'node:module';

// ---------------------------------------------------------------------------
// Tests for the verifySubscription callable Cloud Function.
//
// The auth gate and entitlement-derivation logic are tested OFFLINE (no network,
// no deploy, no real purchase) by importing the pure helpers exported from
// index.js via `exports._internal`.
//
// A real end-to-end verification (calling the live Google Play Developer API with
// a genuine `kinetic_pro` purchase token) is INTENTIONALLY NOT tested here: it
// requires (a) the function to be deployed, (b) the runtime service account to
// have androidpublisher access + Play "API access", and (c) a real purchase of a
// product that does not yet exist. All three are downstream of the merchant
// profile / product creation happening in a parallel session.
//
// An OPTIONAL post-deploy auth-gate check runs only when VERIFY_SUB_URL is set,
// so this file stays green in CI while the function is undeployed.
// ---------------------------------------------------------------------------

const require = createRequire(import.meta.url);
const { _internal } = require('../index.js');
const { requireUid, deriveEntitlementV2, deriveEntitlementV1 } = _internal;

test('requireUid rejects unauthenticated callers', () => {
  // Missing context / auth / uid must throw a callable "unauthenticated" error.
  for (const ctx of [undefined, null, {}, { auth: null }, { auth: {} }, { auth: { uid: '' } }, { auth: { uid: '   ' } }]) {
    assert.throws(
      () => requireUid(ctx),
      (err) => err && err.code === 'unauthenticated',
      `expected unauthenticated for ctx=${JSON.stringify(ctx)}`
    );
  }
});

test('requireUid returns the trimmed uid for authenticated callers', () => {
  assert.strictEqual(requireUid({ auth: { uid: 'user-123' } }), 'user-123');
  assert.strictEqual(requireUid({ auth: { uid: '  user-123  ' } }), 'user-123');
});

test('deriveEntitlementV2 treats ACTIVE and IN_GRACE_PERIOD as entitled', () => {
  assert.strictEqual(deriveEntitlementV2({ subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE' }).active, true);
  assert.strictEqual(deriveEntitlementV2({ subscriptionState: 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD' }).active, true);
});

test('deriveEntitlementV2 treats other states as not entitled', () => {
  for (const state of [
    'SUBSCRIPTION_STATE_EXPIRED',
    'SUBSCRIPTION_STATE_CANCELED',
    'SUBSCRIPTION_STATE_ON_HOLD',
    'SUBSCRIPTION_STATE_PAUSED',
    'SUBSCRIPTION_STATE_PENDING',
    'SUBSCRIPTION_STATE_UNSPECIFIED'
  ]) {
    assert.strictEqual(deriveEntitlementV2({ subscriptionState: state }).active, false, state);
  }
  // Missing state defaults to not-entitled.
  assert.strictEqual(deriveEntitlementV2({}).active, false);
  assert.strictEqual(deriveEntitlementV2(null).active, false);
});

test('deriveEntitlementV2 surfaces the latest lineItem expiry', () => {
  const sub = {
    subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE',
    lineItems: [
      { expiryTime: '2026-01-01T00:00:00Z' },
      { expiryTime: '2026-03-01T00:00:00Z' }
    ]
  };
  const out = deriveEntitlementV2(sub);
  assert.strictEqual(out.expiryTimeMillis, Date.parse('2026-03-01T00:00:00Z'));
});

test('deriveEntitlementV1 fallback: not expired + not pending => entitled', () => {
  const now = 1_000_000;
  const active = deriveEntitlementV1({ expiryTimeMillis: String(now + 60_000), paymentState: 1 }, now);
  assert.strictEqual(active.active, true);
  assert.strictEqual(active.expiryTimeMillis, now + 60_000);

  // Expired.
  assert.strictEqual(deriveEntitlementV1({ expiryTimeMillis: String(now - 1), paymentState: 1 }, now).active, false);
  // Payment pending (paymentState 0) even though not expired.
  assert.strictEqual(deriveEntitlementV1({ expiryTimeMillis: String(now + 60_000), paymentState: 0 }, now).active, false);
  // Missing expiry.
  assert.strictEqual(deriveEntitlementV1({}, now).active, false);
});

// Optional live auth-gate check — only runs post-deploy when VERIFY_SUB_URL is set.
test('verifySubscription callable rejects unauthenticated (live)', async (t) => {
  const url = process.env.VERIFY_SUB_URL;
  if (!url) {
    t.skip('VERIFY_SUB_URL not set (function undeployed); auth gate covered offline by requireUid tests');
    return;
  }
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 30_000);
  let res;
  try {
    res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      // Callable protocol wraps args under "data"; no Authorization header => unauthenticated.
      body: JSON.stringify({ data: { purchaseToken: 'x' } }),
      signal: controller.signal
    });
  } finally {
    clearTimeout(timer);
  }
  assert.strictEqual(
    res.status,
    401,
    `expected 401 for unauthenticated callable, got ${res.status}`
  );
});
