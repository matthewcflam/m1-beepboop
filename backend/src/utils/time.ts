function pad2(n: number): string {
  return n.toString().padStart(2, '0');
}

// Date.getTimezoneOffset() returns minutes the local time is BEHIND UTC
// (e.g. UTC+5:30 -> -330), which is the opposite sign of the GMT offset we
// want to display, so it must be inverted here.
export function formatGmtTime(date: Date): string {
  const hours = pad2(date.getHours());
  const minutes = pad2(date.getMinutes());
  const seconds = pad2(date.getSeconds());

  const offsetMinutesFromUtc = -date.getTimezoneOffset();
  const sign = offsetMinutesFromUtc < 0 ? '-' : '+';
  const absOffset = Math.abs(offsetMinutesFromUtc);
  const offsetHours = pad2(Math.floor(absOffset / 60));
  const offsetMinutes = pad2(absOffset % 60);

  return `${hours}:${minutes}:${seconds} GMT${sign}${offsetHours}:${offsetMinutes}`;
}
