import type { IncomingMessage } from 'http';
import type { Server as HttpServer } from 'http';
import type { Duplex } from 'stream';

import { WebSocket, WebSocketServer } from 'ws';

const UPSTREAM_URL = 'wss://8.229.22.124';
const RELAY_PATH = '/ws/pixels';
const HEARTBEAT_INTERVAL_MS = 25_000;

function relayMessages(source: WebSocket, target: WebSocket): void {
  source.on('message', (data, isBinary) => {
    if (target.readyState === WebSocket.OPEN) {
      target.send(data, { binary: isBinary });
    }
  });
}

function closeTogether(a: WebSocket, b: WebSocket): void {
  const closeBoth = () => {
    if (a.readyState === WebSocket.OPEN || a.readyState === WebSocket.CONNECTING) {
      a.close();
    }
    if (b.readyState === WebSocket.OPEN || b.readyState === WebSocket.CONNECTING) {
      b.close();
    }
  };
  a.on('close', closeBoth);
  a.on('error', closeBoth);
  b.on('close', closeBoth);
  b.on('error', closeBoth);
}

export function attachPixelRelay(server: HttpServer): void {
  const wss = new WebSocketServer({ noServer: true });
  let openClientCount = 0;

  server.on('upgrade', (req: IncomingMessage, socket: Duplex, head: Buffer) => {
    const { pathname } = new URL(req.url ?? '', 'http://localhost');
    if (pathname !== RELAY_PATH) {
      return;
    }

    wss.handleUpgrade(req, socket, head, (client) => {
      const upstream = new WebSocket(UPSTREAM_URL);

      closeTogether(client, upstream);
      relayMessages(upstream, client);

      const heartbeat = setInterval(() => {
        if (client.readyState === WebSocket.OPEN) {
          client.ping();
        }
      }, HEARTBEAT_INTERVAL_MS);

      client.on('close', () => clearInterval(heartbeat));

      openClientCount += 1;
      console.log(`[pixelRelay] client connected (open=${openClientCount})`);
      client.on('close', () => {
        openClientCount -= 1;
        console.log(`[pixelRelay] client disconnected (open=${openClientCount})`);
      });
    });
  });
}
