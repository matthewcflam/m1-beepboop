const IMDS_TOKEN_URL = 'http://169.254.169.254/latest/api/token';
const IMDS_IP_URL = 'http://169.254.169.254/latest/meta-data/public-ipv4';
const IPIFY_URL = 'https://api.ipify.org?format=json';
const IMDS_TIMEOUT_MS = 1000;

let cachedIp: string | null = null;

async function fetchWithTimeout(
  url: string,
  init: RequestInit,
  timeoutMs: number,
): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

async function fetchFromImds(): Promise<string | null> {
  try {
    const tokenRes = await fetchWithTimeout(
      IMDS_TOKEN_URL,
      {
        method: 'PUT',
        headers: { 'X-aws-ec2-metadata-token-ttl-seconds': '21600' },
      },
      IMDS_TIMEOUT_MS,
    );
    if (!tokenRes.ok) return null;
    const token = await tokenRes.text();

    const ipRes = await fetchWithTimeout(
      IMDS_IP_URL,
      { headers: { 'X-aws-ec2-metadata-token': token } },
      IMDS_TIMEOUT_MS,
    );
    if (!ipRes.ok) return null;

    const ip = (await ipRes.text()).trim();
    return ip.length > 0 ? ip : null;
  } catch {
    return null;
  }
}

async function fetchFromIpify(): Promise<string | null> {
  try {
    const res = await fetchWithTimeout(IPIFY_URL, {}, IMDS_TIMEOUT_MS * 5);
    if (!res.ok) return null;
    const body = (await res.json()) as { ip?: string };
    return body.ip ?? null;
  } catch {
    return null;
  }
}

export async function getServerIp(): Promise<string | null> {
  if (cachedIp !== null) return cachedIp;

  const imdsIp = await fetchFromImds();
  if (imdsIp !== null) {
    cachedIp = imdsIp;
    return cachedIp;
  }

  const fallbackIp = await fetchFromIpify();
  if (fallbackIp !== null) {
    cachedIp = fallbackIp;
    return cachedIp;
  }

  return null;
}
