import express, { type Express } from 'express';

import { apiRouter } from './routes/api';

export function createApp(): Express {
  const app = express();

  // nginx runs on the same host in front of this server.
  app.set('trust proxy', 'loopback');

  app.use(express.json());

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.use('/api', apiRouter);

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
