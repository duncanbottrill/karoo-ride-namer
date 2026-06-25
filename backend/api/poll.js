// GET /api/poll?state=… — the Karoo app polls this until the phone-side authorization lands.
// Returns the tokens once (then deletes them), or 204 while still waiting.
import { Redis } from '@upstash/redis';

const redis = new Redis({
  url: process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL,
  token: process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN,
});

export default async function handler(req, res) {
  const state = req.query?.state;
  if (!state) {
    return res.status(400).json({ error: 'missing state' });
  }
  const tokens = await redis.get(`strava:${state}`);
  if (!tokens) {
    return res.status(204).end();
  }
  await redis.del(`strava:${state}`);
  return res.status(200).json(tokens);
}
