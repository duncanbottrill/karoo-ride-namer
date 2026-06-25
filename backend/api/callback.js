// GET /api/callback — Strava redirects here (on the user's PHONE) after they log in and
// authorize. We exchange the code for tokens (secret stays server-side) and stash them in
// Redis keyed by `state`. The Karoo app, which is polling /api/poll?state=…, then picks them
// up. This avoids needing a working browser/login on the Karoo itself.
import { Redis } from '@upstash/redis';

const redis = new Redis({
  url: process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL,
  token: process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN,
});

function page(title, message) {
  return `<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Ride Namer</title>
<style>body{font-family:sans-serif;text-align:center;padding:2.5rem 1.5rem;background:#1B1B1F;color:#fff}
h1{color:#FF5A1F;font-size:1.6rem;margin-bottom:.5rem}p{font-size:1.15rem;line-height:1.55}</style>
</head><body><h1>${title}</h1><p>${message}</p></body></html>`;
}

export default async function handler(req, res) {
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  const { code, state, error } = req.query || {};
  if (error || !code || !state) {
    return res.status(400).send(page('Couldn’t connect',
      error ? `Strava said: ${error}` : 'Missing details — please start again from your Karoo.'));
  }
  const clientId = process.env.STRAVA_CLIENT_ID;
  const clientSecret = process.env.STRAVA_CLIENT_SECRET;
  if (!clientId || !clientSecret) {
    return res.status(500).send(page('Server not configured', 'Strava credentials are missing on the server.'));
  }
  try {
    const params = new URLSearchParams({
      client_id: clientId,
      client_secret: clientSecret,
      code,
      grant_type: 'authorization_code',
    });
    const r = await fetch('https://www.strava.com/oauth/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    });
    const data = await r.json();
    if (!r.ok) {
      return res.status(502).send(page('Strava error', 'Could not complete authorization. Please try again from your Karoo.'));
    }
    await redis.set(
      `strava:${state}`,
      { access_token: data.access_token, refresh_token: data.refresh_token, expires_at: data.expires_at },
      { ex: 600 },
    );
    return res.status(200).send(page('Connected ✓',
      'Strava is connected. Return to your Karoo — it will finish automatically in a few seconds.'));
  } catch (e) {
    return res.status(502).send(page('Error', 'Something went wrong reaching Strava. Please try again.'));
  }
}
