// Security rules tests for audio listening sessions (WIL-58, WIL-64, WIL-65, WIL-110), modeled after mood.rules.test.js
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
  setDoc,
  updateDoc,
} from "firebase/firestore";

const MEMBER = "member-uid";
const OTHER_MEMBER = "other-member-uid";
const ADMIN = "admin-uid";
const SESSION_ID = "session-1";

let env;

const db = (uid) =>
  uid
    ? env.authenticatedContext(uid).firestore()
    : env.unauthenticatedContext().firestore();

const validSession = (userId = MEMBER) => ({
  userId,
  audioId: "track-1",
  completed: true,
  durationSeconds: 300,
});

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-sgula-rules-audio-session",
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
    await setDoc(doc(admin, "AudioSession", SESSION_ID), {
      userId: MEMBER,
      audioId: "track-1",
      completed: true,
      durationSeconds: 300,
      pointsAwarded: false,
    });
  });
});

describe("members can save their own listening sessions", () => {
  test("a member can save a completed session", async () => {
    await assertSucceeds(
      addDoc(collection(db(MEMBER), "AudioSession"), validSession()),
    );
  });

  test("a member can read their own session", async () => {
    await assertSucceeds(getDoc(doc(db(MEMBER), "AudioSession", SESSION_ID)));
  });

  test("a member can update their own session", async () => {
    await assertSucceeds(
      updateDoc(doc(db(MEMBER), "AudioSession", SESSION_ID), {
        durationSeconds: 310,
      }),
    );
  });

  test("a member can delete their own session", async () => {
    await assertSucceeds(
      deleteDoc(doc(db(MEMBER), "AudioSession", SESSION_ID)),
    );
  });
});

describe("sessions are owned securely (WIL-110)", () => {
  test("cannot save a session for someone else", async () => {
    await assertFails(
      addDoc(
        collection(db(MEMBER), "AudioSession"),
        validSession(OTHER_MEMBER),
      ),
    );
  });

  test("signed-out users cannot save a session", async () => {
    await assertFails(
      addDoc(collection(db(null), "AudioSession"), validSession()),
    );
  });

  test("another member cannot read the session", async () => {
    await assertFails(
      getDoc(doc(db(OTHER_MEMBER), "AudioSession", SESSION_ID)),
    );
  });

  test("another member cannot update the session", async () => {
    await assertFails(
      updateDoc(doc(db(OTHER_MEMBER), "AudioSession", SESSION_ID), {
        durationSeconds: 999,
      }),
    );
  });

  test("another member cannot delete the session", async () => {
    await assertFails(
      deleteDoc(doc(db(OTHER_MEMBER), "AudioSession", SESSION_ID)),
    );
  });

  test("cannot move a session to another user", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "AudioSession", SESSION_ID), {
        userId: OTHER_MEMBER,
      }),
    );
  });

  test("a member cannot award themselves points by editing pointsAwarded", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "AudioSession", SESSION_ID), {
        pointsAwarded: true,
      }),
    );
  });
});

describe("admins can see listening activity for engagement, never journal content (WIL-112)", () => {
  test("an admin can read a member's session", async () => {
    await assertSucceeds(getDoc(doc(db(ADMIN), "AudioSession", SESSION_ID)));
  });

  test("an admin can mark points as awarded", async () => {
    await assertSucceeds(
      updateDoc(doc(db(ADMIN), "AudioSession", SESSION_ID), {
        pointsAwarded: true,
      }),
    );
  });

  test("an admin can delete a session", async () => {
    await assertSucceeds(
      deleteDoc(doc(db(ADMIN), "AudioSession", SESSION_ID)),
    );
  });

  test("signed-out users cannot read a session", async () => {
    await assertFails(getDoc(doc(db(null), "AudioSession", SESSION_ID)));
  });
});

/* Reference List
1. Firebase. n.d. Build unit tests. [Online]. Available at: https://firebase.google.com/docs/rules/unit-tests [Accessed 26 September 2026].
2. Firebase. n.d. Test your Cloud Firestore Security Rules. [Online]. Available at: https://firebase.google.com/docs/firestore/security/test-rules-emulator [Accessed 26 September 2026].
3. Node.js. n.d. Test runner. [Online]. Available at: https://nodejs.org/api/test.html [Accessed 26 September 2026].
*/
