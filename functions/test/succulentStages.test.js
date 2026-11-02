// Unit tests for the succulent stage thresholds in src/succulentStages.ts
// Run from the functions folder with npm test

const { describe, test } = require("node:test");
const assert = require("node:assert/strict");
const { nextStreak, stageForPoints } = require("../lib/succulentStages");

describe("stageForPoints", () => {
  test("zero points is a seed", () => {
    assert.equal(stageForPoints(0), "seed");
  });

  test("ninety nine points is still a seed", () => {
    assert.equal(stageForPoints(99), "seed");
  });

  test("one hundred points is a sprout", () => {
    assert.equal(stageForPoints(100), "sprout");
  });

  test("two hundred ninety nine points is still a sprout", () => {
    assert.equal(stageForPoints(299), "sprout");
  });

  test("three hundred points is growing", () => {
    assert.equal(stageForPoints(300), "growing");
  });

  test("five hundred ninety nine points is still growing", () => {
    assert.equal(stageForPoints(599), "growing");
  });

  test("six hundred points is blooming", () => {
    assert.equal(stageForPoints(600), "blooming");
  });

  test("well past six hundred is still blooming", () => {
    assert.equal(stageForPoints(10000), "blooming");
  });
});

// Times are in UTC, Johannesburg is two hours ahead
describe("nextStreak", () => {
  const now = new Date("2026-11-02T10:00:00Z");

  test("the first activity starts a streak of one", () => {
    assert.equal(nextStreak(0, undefined, now), 1);
  });

  test("a second activity on the same day keeps the streak", () => {
    assert.equal(nextStreak(4, new Date("2026-11-02T07:00:00Z"), now), 4);
  });

  test("activity the day after grows the streak", () => {
    assert.equal(nextStreak(4, new Date("2026-11-01T18:00:00Z"), now), 5);
  });

  test("a missed day starts the streak again", () => {
    assert.equal(nextStreak(4, new Date("2026-10-31T18:00:00Z"), now), 1);
  });

  test("days follow Johannesburg time, not UTC", () => {
    // 23:30 UTC on 1 November is already 2 November in Johannesburg
    assert.equal(nextStreak(4, new Date("2026-11-01T23:30:00Z"), now), 4);
  });

  test("an account active today from before streaks existed counts as one", () => {
    assert.equal(nextStreak(0, new Date("2026-11-02T07:00:00Z"), now), 1);
  });
});
