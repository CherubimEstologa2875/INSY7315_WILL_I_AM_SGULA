// Run from this folder with npm install then npm test

import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  Timestamp,
  updateDoc,
  where,
} from "firebase/firestore";

const MEMBER = "member-uid";
const OTHER_MEMBER = "other-member-uid";
const ADMIN = "admin-uid";
const RESULT_ID = "result-1";
const QUIZ_ID = "dailyWellness";

let env;

const db = (uid) =>
  uid
    ? env.authenticatedContext(uid).firestore()
    : env.unauthenticatedContext().firestore();

const result = (userId = MEMBER) => ({
  userId,
  quizId: QUIZ_ID,
  answers: { question1: 2 },
  scores: { sleep: 2 },
  recommendedCategoryId: "sleep",
  recommendedAudioId: "track-1",
  createdAt: Timestamp.now(),
});

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-sgula-rules",
    firestore: {
      rules: readFileSync(
        new URL("../firestore.rules", import.meta.url),
        "utf8",
      ),
    },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const admin = context.firestore();
    await setDoc(doc(admin, "Admin", ADMIN), {
      name: "Practice admin",
      role: "admin",
    });
    await setDoc(doc(admin, "quizResults", RESULT_ID), result());
    await setDoc(doc(admin, "quizQuestions", "question1"), {
      quizId: QUIZ_ID,
      order: 1,
      weight: 2,
      questionText: "How are you feeling right now?",
      options: ["Tense and stressed", "Tired and drained"],
      categoryIds: ["stressRelief", "sleep"],
    });
    await setDoc(doc(admin, "MeditationCategory", "sleep"), {
      name: "Sleep",
      description: "Slow wind-down sessions to help you rest.",
      recommendedFor: "Struggling to fall or stay asleep",
    });
    await setDoc(doc(admin, "AudioContent", "track-1"), {
      title: "Evening wind down",
      category: "Guided",
      categoryId: "sleep",
      storagePath: "audio/track-1",
      durationSeconds: 600,
      active: true,
    });
  });
});

describe("members can take the quiz", () => {
  test("a member can read the quiz questions", async () => {
    const questions = query(
      collection(db(MEMBER), "quizQuestions"),
      where("quizId", "==", QUIZ_ID),
    );
    await assertSucceeds(getDocs(questions));
  });

  test("a member can read the meditation categories", async () => {
    await assertSucceeds(
      getDoc(doc(db(MEMBER), "MeditationCategory", "sleep")),
    );
  });

  test("a member can read their own quiz result", async () => {
    await assertSucceeds(getDoc(doc(db(MEMBER), "quizResults", RESULT_ID)));
  });

  test("a member can list their own quiz results", async () => {
    const own = query(
      collection(db(MEMBER), "quizResults"),
      where("userId", "==", MEMBER),
    );
    await assertSucceeds(getDocs(own));
  });

  test("a member can delete their own quiz result", async () => {
    await assertSucceeds(deleteDoc(doc(db(MEMBER), "quizResults", RESULT_ID)));
  });
});

describe("only the Cloud Function can write quiz results", () => {
  test("a member cannot save their own result", async () => {
    await assertFails(addDoc(collection(db(MEMBER), "quizResults"), result()));
  });

  test("a member cannot change their recommendation", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "quizResults", RESULT_ID), {
        recommendedCategoryId: "focus",
      }),
    );
  });

  test("a member cannot save a result for someone else", async () => {
    await assertFails(
      addDoc(collection(db(MEMBER), "quizResults"), result(OTHER_MEMBER)),
    );
  });

  test("signed-out users cannot save a result", async () => {
    await assertFails(addDoc(collection(db(null), "quizResults"), result()));
  });
});

describe("quiz results stay private", () => {
  test("another member cannot read the result", async () => {
    await assertFails(getDoc(doc(db(OTHER_MEMBER), "quizResults", RESULT_ID)));
  });

  test("another member cannot query it", async () => {
    const memberResults = query(
      collection(db(OTHER_MEMBER), "quizResults"),
      where("userId", "==", MEMBER),
    );
    await assertFails(getDocs(memberResults));
  });

  test("an admin cannot read a member's result", async () => {
    await assertFails(getDoc(doc(db(ADMIN), "quizResults", RESULT_ID)));
  });

  test("signed-out users cannot read it", async () => {
    await assertFails(getDoc(doc(db(null), "quizResults", RESULT_ID)));
  });
});

describe("quiz content is managed by admins", () => {
  test("signed-out users cannot read the questions", async () => {
    await assertFails(getDoc(doc(db(null), "quizQuestions", "question1")));
  });

  test("a member cannot change a question", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "quizQuestions", "question1"), { weight: 10 }),
    );
  });

  test("a member cannot add a meditation category", async () => {
    await assertFails(
      setDoc(doc(db(MEMBER), "MeditationCategory", "custom"), { name: "Mine" }),
    );
  });

  test("a member cannot retag a track to change what gets recommended", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "AudioContent", "track-1"), {
        categoryId: "focus",
      }),
    );
  });

  test("an admin can set a track's wellness category", async () => {
    await assertSucceeds(
      updateDoc(doc(db(ADMIN), "AudioContent", "track-1"), {
        categoryId: "focus",
      }),
    );
  });
});

/* Reference List
1. Firebase. n.d. Build unit tests. [Online]. Available at: https://firebase.google.com/docs/rules/unit-tests [Accessed 23 September 2026].
2. Firebase. n.d. Test your Cloud Firestore Security Rules. [Online]. Available at: https://firebase.google.com/docs/firestore/security/test-rules-emulator [Accessed 23 September 2026].
3. Node.js. n.d. Test runner. [Online]. Available at: https://nodejs.org/api/test.html [Accessed 23 September 2026].
*/
