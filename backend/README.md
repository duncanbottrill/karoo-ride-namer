# Ride Namer — Strava token backend

A tiny serverless backend whose only job is to keep your Strava **client secret** off the
phone. The Karoo app does the Strava login itself, then calls this backend to swap the
OAuth code for tokens (and to refresh them). The secret lives only in this backend's
environment variables — never in the app or the public APK.

## Endpoints

- `GET /api/config` → `{ "clientId": "12345" }` — the public client id (used to build the
  authorize URL).
- `POST /api/token` → exchanges/refreshes tokens. Body is either
  `{ "grant_type": "authorization_code", "code": "..." }` or
  `{ "grant_type": "refresh_token", "refresh_token": "..." }`. Returns
  `{ access_token, refresh_token, expires_at }`.

## Deploy to Vercel

1. Create a Strava API app at <https://www.strava.com/settings/api> and set the
   **Authorization Callback Domain** to `strava-callback`. Note the **Client ID** and
   **Client Secret**.
2. Deploy this folder. Easiest with the Vercel CLI:
   ```bash
   cd backend
   npx vercel            # first run links/creates the project
   npx vercel --prod     # production deploy
   ```
   (Or import the repo in the Vercel dashboard and set **Root Directory** to `backend`.)
3. Add the environment variables in the Vercel project (Settings → Environment Variables,
   or `npx vercel env add`):
   - `STRAVA_CLIENT_ID`
   - `STRAVA_CLIENT_SECRET`

   Redeploy after adding them so they take effect.
4. Note your production URL, e.g. `https://ridenamer-strava.vercel.app`, and put it in the
   app at `app/src/main/kotlin/com/duncanbottrill/ridenamer/strava/StravaConfig.kt`
   (`STRAVA_BACKEND_URL`). It is not secret, so committing it is fine.

## Test it

```bash
curl https://YOUR-PROJECT.vercel.app/api/config
# -> {"clientId":"12345"}
```

## Note on access

The endpoints aren't authenticated, so anyone who knows the URL could call them. They still
can't get your secret or your data — `/api/config` only reveals the (already public) client
id, and `/api/token` needs a valid Strava `code`/`refresh_token` to return anything. The
only practical abuse is using up your Strava API rate limit. For a personal app that's fine;
if you want, add a shared header check later.
