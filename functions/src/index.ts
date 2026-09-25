import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import type { DocumentReference } from "firebase-admin/firestore";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { scoreQuizAnswers, topCategory, type QuizQuestionDoc } from "./quizScoring";

initializeApp();

const db = getFirestore();

type PointsReason = "mood" | "journal" | "quiz" | "audio";

const pointsByReason: Record<PointsReason, number> = {
  mood: 5,
  journal: 10,
  quiz: 15,
  audio: 20,
};

function requireUserId(uid: string | undefined): string {
  if (!uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }
  return uid;
}

function stageForPoints(totalPoints: number): string {
  return totalPoints >= 600 ? "blooming" : totalPoints >= 300 ? "growing" : totalPoints >= 100 ? "sprout" : "seed";
}

/** The only place where points are added; sourceId makes awards idempotent. */
async function awardSucculentPoints(userId: string, reason: PointsReason, sourceId: string, sourceRef?: DocumentReference): Promise<void> {
  const stateRef = db.collection("SucculentState").doc(userId);
  const awardRef = db.collection("SucculentPointAward").doc(`${reason}_${sourceId}`);
  const points = pointsByReason[reason];

  await db.runTransaction(async (transaction) => {
    const awardSnapshot = await transaction.get(awardRef);
    if (awardSnapshot.exists) return;
    const snapshot = await transaction.get(stateRef);
    const current = snapshot.exists ? snapshot.data() ?? {} : {};
    const totalPoints = Number(current.totalPoints ?? 0) + points;
    const stage = stageForPoints(totalPoints);

    transaction.set(stateRef, {
      userId,
      totalPoints,
      stage,
      wilted: false,
      lastActivityAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.create(awardRef, { userId, reason, points, sourceId, createdAt: FieldValue.serverTimestamp() });
    if (sourceRef) transaction.set(sourceRef, { pointsAwarded: true, pointsAwardedAt: FieldValue.serverTimestamp() }, { merge: true });
  });
}

export const onMoodEntryCreated = onDocumentCreated("MoodEntry/{entryId}", async (event) => {
  const userId = event.data?.data()?.userId as string | undefined;
  if (userId) await awardSucculentPoints(userId, "mood", event.params.entryId);
});

export const onJournalEntryCreated = onDocumentCreated("JournalEntry/{entryId}", async (event) => {
  const userId = event.data?.data()?.userId as string | undefined;
  if (userId) await awardSucculentPoints(userId, "journal", event.params.entryId);
});

async function awardCompletedAudio(event: any): Promise<void> {
  const before = event.data?.before.data() ?? {};
  const after = event.data?.after.data() ?? {};
  const userId = after.userId as string | undefined;
  const completed = after.completed === true || after.status === "completed";

  if (!userId || !completed || before.pointsAwarded === true || after.pointsAwarded === true) return;

  await awardSucculentPoints(userId, "audio", event.params.sessionId, event.data?.after.ref);
}

export const onAudioSessionCreated = onDocumentCreated("AudioSession/{sessionId}", async (event) => {
  const data = event.data?.data() ?? {};
  if (data.userId && (data.completed === true || data.status === "completed")) {
    await awardSucculentPoints(data.userId as string, "audio", event.params.sessionId, event.data?.ref);
  }
});

export const onAudioSessionUpdated = onDocumentUpdated("AudioSession/{sessionId}", awardCompletedAudio);

export const onQuizResultCreated = onDocumentCreated("quizResults/{resultId}", async (event) => {
  const userId = event.data?.data()?.userId as string | undefined;
  if (userId) await awardSucculentPoints(userId, "quiz", event.params.resultId);
});

export const calculateQuizRecommendation = onCall(async (request) => {
  const userId = requireUserId(request.auth?.uid);
  const quizId = request.data?.quizId;
  const answers = request.data?.answers;

  if (typeof quizId !== "string" || !answers || typeof answers !== "object" || Array.isArray(answers)) {
    throw new HttpsError("invalid-argument", "quizId and answers are required.");
  }

  const questionSnapshot = await db.collection("quizQuestions").where("quizId", "==", quizId).get();
  if (questionSnapshot.empty) throw new HttpsError("not-found", "This quiz has no questions.");

  const questions = new Map(questionSnapshot.docs.map((doc) => [doc.id, doc.data() as QuizQuestionDoc]));
  const scores = scoreQuizAnswers(questions, answers as Record<string, unknown>);
  const categoryId = scores ? topCategory(scores) : null;
  if (!scores || !categoryId) throw new HttpsError("invalid-argument", "Every question needs one valid answer.");

  const category = (await db.collection("MeditationCategory").doc(categoryId).get()).data() ?? {};
  const tracks = await db.collection("AudioContent")
    .where("categoryId", "==", categoryId)
    .where("active", "==", true)
    .get();
  // Random so retaking the quiz can suggest a different track
  const track = tracks.empty ? null : tracks.docs[Math.floor(Math.random() * tracks.size)];

  const resultRef = db.collection("quizResults").doc();
  await resultRef.set({
    userId,
    quizId,
    answers,
    scores,
    recommendedCategoryId: categoryId,
    recommendedAudioId: track?.id ?? null,
    createdAt: FieldValue.serverTimestamp(),
  });

  return {
    resultId: resultRef.id,
    categoryId,
    categoryName: typeof category.name === "string" ? category.name : categoryId,
    categoryDescription: typeof category.description === "string" ? category.description : "",
    audio: track ? {
      audioId: track.id,
      title: track.get("title") ?? "",
      category: track.get("category") ?? "",
      storagePath: track.get("storagePath") ?? "",
      durationSeconds: track.get("durationSeconds") ?? 0,
    } : null,
  };
});

export const markInactiveSucculentsWilted = onSchedule("every day 00:15", async () => {
  const cutoff = Timestamp.fromMillis(Date.now() - 7 * 24 * 60 * 60 * 1000);
  const snapshot = await db.collection("SucculentState").where("lastActivityAt", "<=", cutoff).limit(500).get();
  if (snapshot.empty) return;
  const batch = db.batch();
  snapshot.docs.filter((doc) => doc.data().wilted !== true).forEach((doc) => batch.set(doc.ref, { wilted: true, updatedAt: FieldValue.serverTimestamp() }, { merge: true }));
  await batch.commit();
});

export const refreshDailyAffirmation = onSchedule("every day 00:05", async () => {
  const snapshot = await db.collection("Affirmation").limit(50).get();
  if (snapshot.empty) return;

  const affirmations: Array<{ id: string; text?: unknown }> = snapshot.docs.map((document) => {
    const data = document.data() as Record<string, unknown>;
    return { id: document.id, text: data.text };
  });
  const affirmation = affirmations[Math.floor(Math.random() * affirmations.length)];
  await db.collection("DailyBroadcast").doc("current").set({
    affirmationId: affirmation.id,
    text: typeof affirmation.text === "string" ? affirmation.text : "Small steps still count.",
    dateDisplayed: new Date().toISOString().slice(0, 10),
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
});

export const sendWellnessReminder = onSchedule("every day 18:00", async () => {
  const users = await db.collection("UserProfile")
    .where("remindersEnabled", "==", true)
    .limit(500)
    .get();

  const batch = db.batch();
  for (const user of users.docs) {
    batch.set(user.ref, {
      lastReminderPreparedAt: FieldValue.serverTimestamp(),
      reminderMessage: "Take a moment for your wellbeing today.",
    }, { merge: true });
  }
  if (!users.empty) await batch.commit();
});
