# Coach Iron Chat Eval — CUA wiring

Scored evaluation of the Kinetic AI Coach `Coach` chat. This is **design + harness only**;
no device run yet. It plugs into the existing CUA test rig (`e2e/cua/run.sh`, a-test).

## Pieces

| File | Role |
|---|---|
| `golden_prompts.json` | Frozen reference: 10 real user prompts + per-prompt rubric, red_flags, weight |
| `rubric.md` | Scoring methodology + pass threshold + canned-reply guard |
| `judge.py` | Scored LLM judge (Azure `gpt-5.4`) + runner → `eval_report.{json,md}` |
| `README.md` | This file — how the CUA test feeds `judge.py` |

## The 10 prompts

program design · squat knee cave (form) · bench plateau (progression) · protein target
(nutrition) · lower-back pain after deadlifts (injury) · skipping workouts (motivation) ·
no-barbell squat substitute · belly-fat cardio (spot-reduction myth) · leg-day warm-up ·
extreme crash-diet request (safety edge case).

## Pass threshold

```
weighted_mean >= 70  AND  min_response_score >= 40  AND  no canned reply
```

## How it wires into the CUA test

```
                    ┌─────────────────── CUA test (a-test, Android) ───────────────────┐
 golden_prompts ──► │ for each prompt:                                                  │
                    │   1. focus Coach chat input                                       │
                    │   2. type prompt, tap Send                                        │
                    │   3. wait for coach bubble (loading spinner clears)               │
                    │   4. ui_dump() → extract latest coach reply text                  │
                    │   5. responses[prompt_id] = reply_text                            │
                    └──────────────────────────┬───────────────────────────────────────┘
                                               │ writes responses.json {id: text}
                                               ▼
                    judge.py --responses responses.json  ─►  eval_report.json + .md
                                               │
                                               ▼
                              PASS/FAIL/INVALID (canned) verdict
```

Reply capture uses a-test's `ui_dump()` / `check_ui_text()` (uiautomator text extraction) —
grab the text of the newest coach chat bubble after each send. Collect all 10 into a flat
`{prompt_id: reply_text}` JSON, then:

```bash
source ~/.env.d/azure-dev.env          # AZURE_DEV_AI_API_KEY / _BASE_URL, model gpt-5.4
python e2e/eval/coach/judge.py --responses /tmp/coach_responses.json \
    --prompts e2e/eval/coach/golden_prompts.json --out-dir /tmp/coach-eval
```

## MUST force the LIVE Gemini path

`GeminiApiClient.askGemini` only hits the real model when it can route through the proxy.
Preconditions the CUA run must satisfy:

1. **Signed in** — a Firebase user must exist so `getIdToken()` returns a token; without it
   the code skips the proxy (`GeminiApiClient.kt:228`).
2. **`FIREBASE_PROXY_URL` configured** (not empty, not `your-project-id`) so `proxyService`
   is non-null (`GeminiApiClient.kt:104`) → request goes to `geminiProxy` → `gemini-3.6-flash`.
3. **Entitlement** — `sendMessage` triggers a paywall if `!isEntitled()`
   (`MainViewModel.kt:297`); the test account must be entitled or the message never sends.

## Canned-reply guard (why the eval can't be fooled)

If the proxy is unconfigured AND `GEMINI_API_KEY == "MY_GEMINI_API_KEY"`, `askGemini` returns
a fixed **offline** string; network failures return a fixed **fallback** string. `judge.py`
detects both (`CANNED_MARKERS`), forces that response to score `0`, and marks the whole run
**INVALID** — so a green result is only possible when the live model actually answered.

## Self-test (proves the judge discriminates)

`judge.py --selftest` scores two hand-written pairs (a good and a bad reply for the protein and
crash-diet prompts) and asserts good > bad on every pair. See the sample output in the handoff
summary. Run before trusting a real eval.
