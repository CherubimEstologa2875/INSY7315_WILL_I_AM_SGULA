// Picks the affirmation for the day. Kept out of index.ts so it can be tested without Firestore.

export type AffirmationOption = { id: string; text: string };

export const FALLBACK_AFFIRMATION = "Small steps still count.";

// Skips yesterday's affirmation whenever there is anything else to choose from, otherwise a
// small pool keeps showing members the same line two days running.
export function pickAffirmation(
  options: AffirmationOption[],
  previousId: string | undefined,
  random: () => number = Math.random,
): AffirmationOption | null {
  const usable = options.filter((option) => option.text.trim().length > 0);
  if (usable.length === 0) return null;
  const fresh = usable.length > 1 ? usable.filter((option) => option.id !== previousId) : usable;
  return fresh[Math.floor(random() * fresh.length)] ?? fresh[0];
}

// The practice and its members are in South Africa, so "today" means today in Johannesburg,
// not UTC. The en-CA locale formats dates as YYYY-MM-DD (MDN, n.d.).
export function johannesburgDate(date: Date): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Africa/Johannesburg",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

/* Reference List
MDN, n.d. Intl.DateTimeFormat. [online] Available at: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat> [Accessed 13 October 2026].
*/
