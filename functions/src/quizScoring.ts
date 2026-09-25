export type QuizQuestionDoc = { weight?: unknown; categoryIds?: unknown };

// Returns null if a question is unanswered or the answer is out of range
export function scoreQuizAnswers(questions: Map<string, QuizQuestionDoc>, answers: Record<string, unknown>): Record<string, number> | null {
  const scores: Record<string, number> = {};
  for (const [questionId, question] of questions) {
    const categoryIds = Array.isArray(question.categoryIds) ? question.categoryIds : [];
    const index = answers[questionId];
    if (typeof index !== "number" || !Number.isInteger(index) || index < 0 || index >= categoryIds.length) return null;
    const categoryId = String(categoryIds[index]);
    const weight = typeof question.weight === "number" ? question.weight : 1;
    scores[categoryId] = (scores[categoryId] ?? 0) + weight;
  }
  return scores;
}

// Ties go to whichever category scored first
export function topCategory(scores: Record<string, number>): string | null {
  return Object.entries(scores).sort((left, right) => right[1] - left[1])[0]?.[0] ?? null;
}