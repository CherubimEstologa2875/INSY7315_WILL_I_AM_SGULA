// Stage thresholds, matched against docs/database-design.md and the client's copy in
// SucculentStages.kt (seed <100, sprout 100-299, growing 300-599, blooming 600+)
export function stageForPoints(totalPoints: number): string {
  return totalPoints >= 600 ? "blooming" : totalPoints >= 300 ? "growing" : totalPoints >= 100 ? "sprout" : "seed";
}
