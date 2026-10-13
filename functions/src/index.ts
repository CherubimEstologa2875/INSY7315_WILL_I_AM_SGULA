import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import type { DocumentReference } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall, onRequest, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { setGlobalOptions } from "firebase-functions/v2";
import { scoreQuizAnswers, topCategory, type QuizQuestionDoc } from "./quizScoring";
import { FALLBACK_AFFIRMATION, johannesburgDate, pickAffirmation } from "./dailyAffirmation";

// Has to match the Firestore database location or the triggers won't deploy
setGlobalOptions({ region: "africa-south1" });

// Cloud Scheduler isn't available in africa-south1
const SCHEDULER_REGION = "europe-west1";

// Without a time zone the Firebase CLI deploys these schedules on UTC, which put the "midnight"
// affirmation refresh at 02:05 here and the 18:00 reminder at 20:00 (Firebase, 2026).
const SCHEDULER_TIME_ZONE = "Africa/Johannesburg";

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
      downloadUrl: track.get("downloadUrl") ?? "",
      durationSeconds: track.get("durationSeconds") ?? 0,
    } : null,
  };
});

// Read-only REST API: GET /broadcasts and GET /quiz/:quizId, both need the user's Firebase ID token
export const api = onRequest(async (req, res) => {
  if (req.method !== "GET") {
    res.set("Allow", "GET").status(405).json({ error: "Only GET is supported." });
    return;
  }

  const token = req.get("Authorization")?.match(/^Bearer (.+)$/)?.[1];
  if (!token) {
    res.status(401).json({ error: "Sign in first." });
    return;
  }
  try {
    await getAuth().verifyIdToken(token);
  } catch {
    res.status(401).json({ error: "Your session has expired." });
    return;
  }

  const [resource, id, extra] = req.path.split("/").filter(Boolean);
  try {
    if (resource === "broadcasts" && !id) {
      const snapshot = await db.collection("Broadcast").orderBy("publishedAt", "desc").get();
      res.status(200).json({
        broadcasts: snapshot.docs.map((doc) => ({
          broadcastId: doc.id,
          title: doc.get("title") ?? "",
          message: doc.get("message") ?? "",
          author: doc.get("author") ?? "",
          publishedAtMillis: (doc.get("publishedAt") as Timestamp | undefined)?.toMillis() ?? null,
          storagePath: doc.get("storagePath") ?? "",
          durationSeconds: doc.get("durationSeconds") ?? 0,
        })),
      });
      return;
    }

    if (resource === "quiz" && id && !extra) {
      const snapshot = await db.collection("quizQuestions").where("quizId", "==", id).get();
      if (snapshot.empty) {
        res.status(404).json({ error: `No quiz called ${id}.` });
        return;
      }
      const questions = snapshot.docs
        .map((doc) => ({
          questionId: doc.id,
          questionText: doc.get("questionText") ?? "",
          options: doc.get("options") ?? [],
          categoryIds: doc.get("categoryIds") ?? [],
          weight: doc.get("weight") ?? 1,
          order: doc.get("order") ?? 0,
        }))
        .sort((a, b) => a.order - b.order);
      res.status(200).json({ quizId: id, questions });
      return;
    }

    res.status(404).json({ error: "No such route." });
  } catch (error) {
    logger.error("api request failed", req.path, error);
    res.status(500).json({ error: "Something went wrong on the server." });
  }
});

export const markInactiveSucculentsWilted = onSchedule({ schedule: "every day 00:15", timeZone: SCHEDULER_TIME_ZONE, region: SCHEDULER_REGION }, async () => {
  const cutoff = Timestamp.fromMillis(Date.now() - 7 * 24 * 60 * 60 * 1000);
  const snapshot = await db.collection("SucculentState").where("lastActivityAt", "<=", cutoff).limit(500).get();
  if (snapshot.empty) return;
  const batch = db.batch();
  snapshot.docs.filter((doc) => doc.data().wilted !== true).forEach((doc) => batch.set(doc.ref, { wilted: true, updatedAt: FieldValue.serverTimestamp() }, { merge: true }));
  await batch.commit();
});

// The whole pool is read, not just the first 50, so affirmations added later still get a turn.
// select() only pulls the text field, which keeps the read small (Google Cloud, n.d.).
export const refreshDailyAffirmation = onSchedule({ schedule: "every day 00:05", timeZone: SCHEDULER_TIME_ZONE, region: SCHEDULER_REGION }, async () => {
  const currentRef = db.collection("DailyBroadcast").doc("current");
  const [snapshot, current] = await Promise.all([db.collection("Affirmation").select("text").get(), currentRef.get()]);

  const options = snapshot.docs.map((document) => {
    const text = document.get("text");
    return { id: document.id, text: typeof text === "string" ? text : "" };
  });
  const affirmation = pickAffirmation(options, current.get("affirmationId") as string | undefined);

  await currentRef.set({
    affirmationId: affirmation?.id ?? "",
    text: affirmation?.text.trim() ?? FALLBACK_AFFIRMATION,
    dateDisplayed: johannesburgDate(new Date()),
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
});

export const sendWellnessReminder = onSchedule({ schedule: "every day 18:00", timeZone: SCHEDULER_TIME_ZONE, region: SCHEDULER_REGION }, async () => {
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

/* Reference List
Firebase, 2026. firebase-tools: src/gcp/cloudscheduler.ts. [online] Available at: <https://github.com/firebase/firebase-tools/blob/master/src/gcp/cloudscheduler.ts> [Accessed 13 October 2026].
Firebase, n.d. Schedule functions. [online] Available at: <https://firebase.google.com/docs/functions/schedule-functions> [Accessed 13 October 2026].
Google Cloud, n.d. Class Query (Node.js client library for Firestore). [online] Available at: <https://cloud.google.com/nodejs/docs/reference/firestore/latest/firestore/query> [Accessed 13 October 2026].
*/
