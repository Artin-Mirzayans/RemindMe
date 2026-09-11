// picks the timestamp to prefill: the recommended reminder time if it's still in the future,
// else the event's own start time if that's still future, else null so the user picks
const firstFuture = (...candidates: (string | null | undefined)[]): string | null => {
  const now = Date.now();
  for (const candidate of candidates) {
    if (!candidate) continue;
    const ms = new Date(candidate).getTime();
    if (!Number.isNaN(ms) && ms > now) return candidate;
  }
  return null;
};

export default firstFuture;
