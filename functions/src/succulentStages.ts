// Stage thresholds, matched against docs/database-design.md and the client's copy in
// SucculentStages.kt (seed <100, sprout 100-299, growing 300-599, blooming 600+)
import { johannesburgDate } from "./dailyAffirmation";

export function stageForPoints(totalPoints: number): string {
  return totalPoints >= 600 ? "blooming" : totalPoints >= 300 ? "growing" : totalPoints >= 100 ? "sprout" : "seed";
}

const DAY_MS = 24 * 60 * 60 * 1000;

// Days in a row with at least one activity, matched against currentStreak in SucculentStages.kt
export function nextStreak(previous: number, lastActivity: Date | undefined, now: Date): number {
  if (!lastActivity) return 1;
  const lastDay = johannesburgDate(lastActivity);
  if (lastDay === johannesburgDate(now)) return Math.max(previous, 1);
  if (lastDay === johannesburgDate(new Date(now.getTime() - DAY_MS))) return previous + 1;
  return 1;
}
