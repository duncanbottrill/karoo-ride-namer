// POST /api/token — proxies Strava's OAuth token exchange so the client secret stays
// server-side. Body: { grant_type: "authorization_code", code } or
// { grant_type: "refresh_token", refresh_token }. Returns { access_token, refresh_token,
// expires_at }.
export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Use POST' });
    return;
  }
  const clientId = process.env.STRAVA_CLIENT_ID;
  const clientSecret = process.env.STRAVA_CLIENT_SECRET;
  if (!clientId || !clientSecret) {
    res.status(500).json({ error: 'Strava credentials not configured on the server' });
    return;
  }

  const body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : (req.body || {});
  const params = new URLSearchParams({ client_id: clientId, client_secret: clientSecret });

  if (body.grant_type === 'authorization_code') {
    if (!body.code) { res.status(400).json({ error: 'missing code' }); return; }
    params.set('grant_type', 'authorization_code');
    params.set('code', body.code);
  } else if (body.grant_type === 'refresh_token') {
    if (!body.refresh_token) { res.status(400).json({ error: 'missing refresh_token' }); return; }
    params.set('grant_type', 'refresh_token');
    params.set('refresh_token', body.refresh_token);
  } else {
    res.status(400).json({ error: 'grant_type must be authorization_code or refresh_token' });
    return;
  }

  try {
    const stravaRes = await fetch('https://www.strava.com/oauth/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    });
    const data = await stravaRes.json();
    if (!stravaRes.ok) {
      res.status(stravaRes.status).json(data);
      return;
    }
    // Only return tokens — never echo the secret or athlete PII.
    res.status(200).json({
      access_token: data.access_token,
      refresh_token: data.refresh_token,
      expires_at: data.expires_at,
    });
  } catch (e) {
    res.status(502).json({ error: 'Failed to reach Strava', detail: String(e) });
  }
}
