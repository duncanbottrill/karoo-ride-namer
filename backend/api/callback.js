// GET /api/callback — Strava redirects here after authorization (Strava only allows real
// https domains as the callback, not custom URI schemes). We immediately bounce the browser
// to the app's custom scheme, preserving the query (code / scope / state / error), which the
// Karoo opens via its intent-filter.
export default function handler(req, res) {
  const qs = req.url.includes('?') ? req.url.slice(req.url.indexOf('?') + 1) : '';
  res.statusCode = 302;
  res.setHeader('Location', `ridenamer://strava-callback?${qs}`);
  res.end();
}
