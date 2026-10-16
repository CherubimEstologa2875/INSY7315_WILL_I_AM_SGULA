// Unit tests for the succulent stage thresholds in src/succulentStages.ts
// Run from the functions folder with npm test

const { describe, test } = require("node:test");
const assert = require("node:assert/strict");
const { stageForPoints } = require("../lib/succulentStages");

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
