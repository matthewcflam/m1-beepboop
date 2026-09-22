import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

export const env = {
  port,
  nodeEnv: process.env.NODE_ENV ?? 'development',
  firstName: process.env.STUDENT_FIRST_NAME ?? 'Matthew',
  lastName: process.env.STUDENT_LAST_NAME ?? 'Lam',
} as const;
