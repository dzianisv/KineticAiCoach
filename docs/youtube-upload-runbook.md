# YouTube Upload Runbook

## Status (as of 2026-07-28): PARTIAL — OAuth client created, refresh token still pending

`client_id` and `client_secret` exist and are stored in Bitwarden collection
`dev`, item `kinetic-ai-coach-youtube-upload-oauth` (id `98d52d00-2a77-426b-854d-b4950147ca5f`).
A refresh token for the `youtube.upload` scope has NOT been generated yet — do
not assume fully automated upload is ready until that step is done too.

## What happened

- Enabled/confirmed "YouTube Data API v3" on project `kinetic-ai-coach-50627`
  (existing GCP project, same one used for Firebase — see
  `app/google-services.json`) — already showed "API Enabled" when checked.
- OAuth consent screen ("Google Auth Platform") was already configured for
  this project (Overview page showed a healthy project checkup — billing,
  contacts, client usage all green) — no consent-screen setup was needed.
- Created a new OAuth 2.0 Client ID, type "Desktop app", name
  "Kinetic Demo Upload CLI", via Google Auth Platform > Clients > Create,
  authenticated as `vibeteaichnologies@gmail.com` (confirmed via the account
  button in the console UI before creating anything).
- Stored `client_id` and `client_secret` in Bitwarden `dev` collection,
  item `kinetic-ai-coach-youtube-upload-oauth`. Notes field documents the
  project, account, and creation method for future reference.
- Prior blocker (Chrome's default session was on the personal
  `dzianisvv@gmail.com` account) was resolved by using the browser's
  multi-account session: `vibeteaichnologies@gmail.com` was already
  signed in as a secondary Google account (`?authuser=3`) in the same
  browser profile — no password was needed or used.

## What's still needed

1. Generate a refresh token for scope
   `https://www.googleapis.com/auth/youtube.upload` using a local script
   (`google-auth-oauthlib`'s installed-app flow) run interactively once,
   approving as `vibeteaichnologies@gmail.com`. This step needs a real script,
   not pure browser clicks — the auth code exchange isn't a UI action.
2. Store the resulting refresh token in the `refresh_token` custom field on
   the same Bitwarden item (currently set to placeholder `NOT_YET_GENERATED`).
3. Once the refresh token exists, future uploads can use the API pattern
   below with zero browser automation.


## Future automated upload approach (once credentials exist)

Do not use browser automation for uploads. Use the official API:

```python
from googleapiclient.discovery import build
from google.oauth2.credentials import Credentials

creds = Credentials(
    None,
    refresh_token=REFRESH_TOKEN,        # from Bitwarden
    client_id=CLIENT_ID,                # from Bitwarden
    client_secret=CLIENT_SECRET,        # from Bitwarden
    token_uri="https://oauth2.googleapis.com/token",
    scopes=["https://www.googleapis.com/auth/youtube.upload"],
)

youtube = build("youtube", "v3", credentials=creds)
youtube.videos().insert(
    part="snippet,status",
    body={
        "snippet": {"title": "...", "description": "..."},
        "status": {"privacyStatus": "private"},
    },
    media_body="path/to/video.mp4",
).execute()
```

Pull all four credential values from Bitwarden (`bw get item
kinetic-ai-coach-youtube-upload-oauth`) at run time — never hardcode or
commit them.
