import { Router } from 'express';

import { env } from '../config/env';
import { getServerIp } from '../services/serverIp';
import { formatGmtTime } from '../utils/time';

export const apiRouter = Router();

apiRouter.get('/server-ip', async (_req, res) => {
  const ip = await getServerIp();
  if (ip === null) {
    res.status(503).json({ error: 'Could not determine server IP' });
    return;
  }
  res.json({ ip });
});

apiRouter.get('/server-time', (_req, res) => {
  res.json({ time: formatGmtTime(new Date()) });
});

apiRouter.get('/name', (_req, res) => {
  res.json({ firstName: env.firstName, lastName: env.lastName });
});

apiRouter.get('/client-ip', (req, res) => {
  const ip = req.ip?.replace(/^::ffff:/, '') ?? '';
  res.json({ ip });
});
