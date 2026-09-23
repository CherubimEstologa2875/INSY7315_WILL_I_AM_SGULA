// Unit tests for the quiz scoring in src/quizScoring.ts
// Run from the functions folder with npm test

const { describe, test } = require("node:test");
const assert = require("node:assert/strict");
const { scoreQuizAnswers, topCategory } = require("../lib/quizScoring");

const questions = new Map([
  ["question1", { weight: 2, categoryIds: ["stressRelief", "sleep", "focus"] }],
  ["question2", { weight: 1, categoryIds: ["sleep", "focus", "stressRelief"] }],
  ["question3", { weight: 1, categoryIds: ["focus", "breathing", "sleep"] }],
]);

describe("scoreQuizAnswers", () => {
  test("adds each question's weight to the chosen category", () => {
    const scores = scoreQuizAnswers(questions, { question1: 1, question2: 0, question3: 1 });
    assert.deepEqual(scores, { sleep: 3, breathing: 1 });
  });

  test("uses a weight of 1 when a question has none", () => {
    const unweighted = new Map([["question1", { categoryIds: ["sleep", "focus"] }]]);
    assert.deepEqual(scoreQuizAnswers(unweighted, { question1: 0 }), { sleep: 1 });
  });

  test("rejects a missing answer", () => {
    assert.equal(scoreQuizAnswers(questions, { question1: 0, question2: 0 }), null);
  });

  test("rejects an answer that is out of range", () => {
    assert.equal(scoreQuizAnswers(questions, { question1: 3, question2: 0, question3: 0 }), null);
    assert.equal(scoreQuizAnswers(questions, { question1: -1, question2: 0, question3: 0 }), null);
  });

  test("rejects answers that are not whole numbers", () => {
    assert.equal(scoreQuizAnswers(questions, { question1: "0", question2: 0, question3: 0 }), null);
    assert.equal(scoreQuizAnswers(questions, { question1: 0.5, question2: 0, question3: 0 }), null);
  });
});

describe("topCategory", () => {
  test("picks the highest score", () => {
    assert.equal(topCategory({ sleep: 3, focus: 4, breathing: 1 }), "focus");
  });

  test("a weighted answer beats single answers split across categories", () => {
    const scores = scoreQuizAnswers(questions, { question1: 0, question2: 1, question3: 1 });
    assert.equal(topCategory(scores), "stressRelief");
  });

  test("a tie goes to the category that scored first", () => {
    assert.equal(topCategory({ sleep: 2, focus: 2 }), "sleep");
  });

  test("returns null when nothing scored", () => {
    assert.equal(topCategory({}), null);
  });
});

/* Reference List
1. Node.js. n.d. Test runner. [Online]. Available at: https://nodejs.org/api/test.html [Accessed 23 September 2026].
2. Node.js. n.d. Assert. [Online]. Available at: https://nodejs.org/api/assert.html [Accessed 23 September 2026].
*/