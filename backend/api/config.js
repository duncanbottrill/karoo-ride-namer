// GET /api/config — returns the public Strava client id so the app can build the
// authorize URL. The client id is not secret (it appears in the OAuth redirect anyway);
// the client SECRET never leaves this server.
export default function handler(req, res) {
  const clientId = process.env.STRAVA_CLIENT_ID;
  if (!clientId) {
    res.status(500).json({ error: 'STRAVA_CLIENT_ID env var not set' });
    return;
  }
  res.status(200).json({ clientId });
}
