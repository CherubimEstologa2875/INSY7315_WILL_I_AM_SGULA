// Unit tests for picking the daily affirmation in src/dailyAffirmation.ts (WIL-168)
// Run from the functions folder with npm test

const { describe, test } = require("node:test");
const assert = require("node:assert/strict");
const { pickAffirmation, johannesburgDate } = require("../lib/dailyAffirmation");

const pool = [
  { id: "a1", text: "Small steps still count." },
  { id: "a2", text: "I am allowed to rest." },
  { id: "a3", text: "I am allowed to grow at my own pace." },
];

describe("pickAffirmation", () => {
  test("never repeats yesterday's affirmation while there are others", () => {
    for (const roll of [0, 0.34, 0.5, 0.67, 0.99]) {
      assert.notEqual(pickAffirmation(pool, "a1", () => roll).id, "a1");
    }
  });

  test("can reach every other affirmation in the pool", () => {
    const picked = new Set([0, 0.5, 0.99].map((roll) => pickAffirmation(pool, "a1", () => roll).id));
    assert.deepEqual([...picked].sort(), ["a2", "a3"]);
  });

  test("keeps showing the only affirmation there is", () => {
    assert.equal(pickAffirmation([pool[0]], "a1", () => 0.5).id, "a1");
  });

  test("skips blank affirmations", () => {
    const withBlank = [{ id: "blank", text: "   " }, pool[1]];
    assert.equal(pickAffirmation(withBlank, undefined, () => 0).id, "a2");
  });

  test("returns null when the pool is empty", () => {
    assert.equal(pickAffirmation([], undefined), null);
    assert.equal(pickAffirmation([{ id: "blank", text: "" }], undefined), null);
  });
});

describe("johannesburgDate", () => {
  test("uses the South African date, not the UTC one", () => {
    // 23:30 UTC on 12 October is already 01:30 on 13 October in Johannesburg
    assert.equal(johannesburgDate(new Date("2026-10-12T23:30:00Z")), "2026-10-13");
    assert.equal(johannesburgDate(new Date("2026-10-13T10:00:00Z")), "2026-10-13");
  });
});

/* Reference List
Node.js, n.d. Test runner. [online] Available at: <https://nodejs.org/api/test.html> [Accessed 13 October 2026].
*/
