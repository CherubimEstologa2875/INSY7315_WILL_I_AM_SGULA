import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";

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

async function awardSucculentPoints(userId: string, reason: PointsReason): Promise<void> {
  const stateRef = db.collection("SucculentState").doc(userId);
  const points = pointsByReason[reason];

  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(stateRef);
    const current = snapshot.exists ? snapshot.data() ?? {} : {};
    const totalPoints = Number(current.totalPoints ?? 0) + points;
    const stage = totalPoints >= 600
      ? "blooming"
      : totalPoints >= 300
        ? "growing"
        : totalPoints >= 100
          ? "sprout"
          : "seed";

    transaction.set(stateRef, {
      userId,
      totalPoints,
      stage,
      lastActivityAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
}

export const onMoodEntryCreated = onDocumentCreated("MoodEntry/{entryId}", async (event) => {
  const userId = event.data?.data()?.userId as string | undefined;
  if (userId) await awardSucculentPoints(userId, "mood");
});

export const onJournalEntryCreated = onDocumentCreated("JournalEntry/{entryId}", async (event) => {
  const userId = event.data?.data()?.userId as string | undefined;
  if (userId) await awardSucculentPoints(userId, "journal");
});

export const onAudioSessionUpdated = onDocumentUpdated("AudioSession/{sessionId}", async (event) => {
  const before = event.data?.before.data() ?? {};
  const after = event.data?.after.data() ?? {};
  const userId = after.userId as string | undefined;
  const completed = after.completed === true || after.status === "completed";

  if (!userId || !completed || before.pointsAwarded === true || after.pointsAwarded === true) return;

  await awardSucculentPoints(userId, "audio");
  await event.data?.after.ref.set({
    pointsAwarded: true,
    pointsAwardedAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
});

export const calculateQuizRecommendation = onCall(async (request) => {
  const userId = requireUserId(request.auth?.uid);
  const answers = request.data?.answers;

  if (!answers || typeof answers !== "object" || Array.isArray(answers)) {
    throw new HttpsError("invalid-argument", "answers must be an object.");
  }

  const scores: Record<string, number> = {};
  for (const value of Object.values(answers as Record<string, unknown>)) {
    if (typeof value !== "string" || value.trim().length === 0) {
      throw new HttpsError("invalid-argument", "Each answer must be a non-empty category.");
    }
    scores[value] = (scores[value] ?? 0) + 1;
  }

  const recommendedCategoryId = Object.entries(scores)
    .sort((left, right) => right[1] - left[1])[0]?.[0] ?? "mindfulness";

  const resultRef = db.collection("quizResults").doc();
  await resultRef.set({
    resultId: resultRef.id,
    userId,
    recommendedCategoryId,
    scores,
    createdAt: Timestamp.now(),
  });
  await awardSucculentPoints(userId, "quiz");

  return { resultId: resultRef.id, recommendedCategoryId, scores };
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
