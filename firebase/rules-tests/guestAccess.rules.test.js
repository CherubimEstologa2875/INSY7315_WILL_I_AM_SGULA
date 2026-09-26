// Security rules tests for the guest experience (guests can listen without an account, WIL-111), modeled after mood.rules.test.js
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
  setDoc,
  updateDoc,
} from "firebase/firestore";

const MEMBER = "member-uid";
const ADMIN = "admin-uid";
const TRACK_ID = "track-1";
const BROADCAST_ID = "broadcast-1";
const DAILY_ID = "daily-1";
const RELAXATION_ID = "relax-1";
const CATEGORY_ID = "category-1";

let env;

const db = (uid) =>
  uid
    ? env.authenticatedContext(uid).firestore()
    : env.unauthenticatedContext().firestore();

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-sgula-rules-guest",
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
    await setDoc(doc(admin, "AudioContent", TRACK_ID), {
      title: "Evening wind down",
      category: "Guided meditation",
      storagePath: "audio/evening.mp3",
      durationSeconds: 600,
      active: true,
    });
    await setDoc(doc(admin, "Broadcast", BROADCAST_ID), {
      title: "Managing Anxiety",
      message: "A short check-in for the morning.",
      publishedAtMillis: Date.now(),
    });
    await setDoc(doc(admin, "DailyBroadcast", DAILY_ID), {
      title: "Today's affirmation",
      message: "You are allowed to rest.",
    });
    await setDoc(doc(admin, "relaxationTracks", RELAXATION_ID), {
      title: "Rain on a tin roof",
      category: "Nature sounds",
    });
    await setDoc(doc(admin, "MeditationCategory", CATEGORY_ID), {
      name: "Calm reset",
    });
  });
});

describe("guests can browse the audio and broadcast library without an account (WIL-111)", () => {
  test("a signed-out user can read a track", async () => {
    await assertSucceeds(getDoc(doc(db(null), "AudioContent", TRACK_ID)));
  });

  test("a signed-out user can list the audio library", async () => {
    await assertSucceeds(getDocs(collection(db(null), "AudioContent")));
  });

  test("a signed-out user can read today's broadcast", async () => {
    await assertSucceeds(getDoc(doc(db(null), "Broadcast", BROADCAST_ID)));
  });

  test("a signed-out user can read the daily broadcast", async () => {
    await assertSucceeds(getDoc(doc(db(null), "DailyBroadcast", DAILY_ID)));
  });

  test("a signed-out user can read relaxation tracks", async () => {
    await assertSucceeds(
      getDoc(doc(db(null), "relaxationTracks", RELAXATION_ID)),
    );
  });
});

describe("only admins can change the public library", () => {
  test("a signed-out user cannot add a track", async () => {
    await assertFails(
      addDoc(collection(db(null), "AudioContent"), {
        title: "Sneaky upload",
        category: "Nature sounds",
      }),
    );
  });

  test("a signed-in member cannot add a track either", async () => {
    await assertFails(
      addDoc(collection(db(MEMBER), "AudioContent"), {
        title: "Sneaky upload",
        category: "Nature sounds",
      }),
    );
  });

  test("a signed-in member cannot edit a broadcast", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "Broadcast", BROADCAST_ID), {
        title: "Edited",
      }),
    );
  });

  test("a signed-in member cannot delete a relaxation track", async () => {
    await assertFails(
      deleteDoc(doc(db(MEMBER), "relaxationTracks", RELAXATION_ID)),
    );
  });

  test("an admin can add a track", async () => {
    await assertSucceeds(
      addDoc(collection(db(ADMIN), "AudioContent"), {
        title: "New upload",
        category: "White noise",
      }),
    );
  });
});

describe("some content needs an account even though it isn't private", () => {
  test("a signed-out user cannot read meditation categories", async () => {
    await assertFails(
      getDoc(doc(db(null), "MeditationCategory", CATEGORY_ID)),
    );
  });

  test("a signed-in member can read meditation categories", async () => {
    await assertSucceeds(
      getDoc(doc(db(MEMBER), "MeditationCategory", CATEGORY_ID)),
    );
  });
});

describe("guests cannot reach member-only data or actions", () => {
  test("a signed-out user cannot create a mood entry", async () => {
    await assertFails(
      addDoc(collection(db(null), "MoodEntry"), {
        userId: "guest-uid",
        moodLevel: 3,
        note: "",
        createdAt: new Date(),
      }),
    );
  });

  test("a signed-out user cannot create a journal entry", async () => {
    await assertFails(
      addDoc(collection(db(null), "JournalEntry"), {
        userId: "guest-uid",
        content: "Trying to write without an account",
        prompt: "What made you feel calm today?",
        createdAt: new Date(),
        updatedAt: new Date(),
      }),
    );
  });

  test("a signed-out user cannot start a listening session", async () => {
    await assertFails(
      addDoc(collection(db(null), "AudioSession"), {
        userId: "guest-uid",
        audioId: TRACK_ID,
        completed: true,
        durationSeconds: 60,
      }),
    );
  });

  test("a signed-out user cannot read a member's profile", async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "UserProfile", MEMBER), {
        userId: MEMBER,
        email: "member@sgula.test",
        displayName: "Test Member",
        role: "member",
      });
    });
    await assertFails(getDoc(doc(db(null), "UserProfile", MEMBER)));
  });
});

/* Reference List
1. Firebase. n.d. Build unit tests. [Online]. Available at: https://firebase.google.com/docs/rules/unit-tests [Accessed 26 September 2026].
2. Firebase. n.d. Test your Cloud Firestore Security Rules. [Online]. Available at: https://firebase.google.com/docs/firestore/security/test-rules-emulator [Accessed 26 September 2026].
3. Node.js. n.d. Test runner. [Online]. Available at: https://nodejs.org/api/test.html [Accessed 26 September 2026].
*/
