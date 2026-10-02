// Security rules tests for therapist broadcasts (IIE, 2026): only the practice can upload or schedule them,
// while clients may read the schedule. Run from this folder: npm install, then npm test.
// Uses @firebase/rules-unit-testing against the emulator (Firebase, n.d.) with Node's test runner (Node.js, n.d.).

import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  setDoc,
  Timestamp,
  updateDoc,
} from "firebase/firestore";

const MEMBER = "member-uid";
const ADMIN = "admin-uid";
const BROADCAST_ID = "broadcast-1";
const NEXT_WEEK = Timestamp.fromMillis(Date.now() + 7 * 24 * 60 * 60 * 1000);

let env;

const db = (uid) => (uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore());

const validBroadcast = (publishedAt = NEXT_WEEK) => ({
  broadcastId: "broadcast-2",
  title: "Managing anxiety",
  message: "A short reset for the morning.",
  author: "Anat Casey",
  publishedAt,
  storagePath: "broadcasts/broadcast-2",
  durationSeconds: 420,
  createdAt: Timestamp.now(),
});

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-sgula-rules",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const admin = context.firestore();
    await setDoc(doc(admin, "Admin", ADMIN), { name: "Practice admin", role: "admin" });
    await setDoc(doc(admin, "Broadcast", BROADCAST_ID), {
      broadcastId: BROADCAST_ID,
      title: "Sleeping through worry",
      message: "Winding the body down.",
      author: "Anat Casey",
      publishedAt: Timestamp.fromMillis(Date.now() - 1000),
      storagePath: `broadcasts/${BROADCAST_ID}`,
      durationSeconds: 300,
      createdAt: Timestamp.now(),
    });
  });
});

describe("only the practice can publish broadcasts", () => {
  test("an admin can schedule a broadcast for a future date", async () => {
    await assertSucceeds(setDoc(doc(db(ADMIN), "Broadcast", "broadcast-2"), validBroadcast()));
  });

  test("an admin can reschedule and delete a broadcast", async () => {
    await assertSucceeds(
      setDoc(doc(db(ADMIN), "Broadcast", BROADCAST_ID), { ...validBroadcast(), broadcastId: BROADCAST_ID }),
    );
    await assertSucceeds(deleteDoc(doc(db(ADMIN), "Broadcast", BROADCAST_ID)));
  });

  test("a member cannot upload a broadcast", async () => {
    await assertFails(setDoc(doc(db(MEMBER), "Broadcast", "broadcast-2"), validBroadcast()));
  });

  test("a member cannot edit or delete one", async () => {
    await assertFails(updateDoc(doc(db(MEMBER), "Broadcast", BROADCAST_ID), { title: "Changed" }));
    await assertFails(deleteDoc(doc(db(MEMBER), "Broadcast", BROADCAST_ID)));
  });

  test("a signed-out visitor cannot upload one", async () => {
    await assertFails(setDoc(doc(db(null), "Broadcast", "broadcast-2"), validBroadcast()));
  });
});

describe("broadcast documents must be well formed", () => {
  test("a broadcast needs a title", async () => {
    await assertFails(setDoc(doc(db(ADMIN), "Broadcast", "broadcast-2"), { ...validBroadcast(), title: "" }));
  });

  test("a broadcast needs a real scheduled time", async () => {
    await assertFails(
      setDoc(doc(db(ADMIN), "Broadcast", "broadcast-2"), { ...validBroadcast(), publishedAt: "next week" }),
    );
  });

  test("unexpected fields are rejected", async () => {
    await assertFails(
      setDoc(doc(db(ADMIN), "Broadcast", "broadcast-2"), { ...validBroadcast(), clientNotes: "private" }),
    );
  });

  test("an over-long presenter name is rejected", async () => {
    await assertFails(
      setDoc(doc(db(ADMIN), "Broadcast", "broadcast-2"), { ...validBroadcast(), author: "a".repeat(121) }),
    );
  });

  test("a negative duration is rejected", async () => {
    await assertFails(
      setDoc(doc(db(ADMIN), "Broadcast", "broadcast-2"), { ...validBroadcast(), durationSeconds: -1 }),
    );
  });
});

describe("clients can read the broadcast schedule", () => {
  test("a member can read a broadcast", async () => {
    await assertSucceeds(getDoc(doc(db(MEMBER), "Broadcast", BROADCAST_ID)));
  });

  test("a member can list broadcasts", async () => {
    await assertSucceeds(getDocs(collection(db(MEMBER), "Broadcast")));
  });
});

/* Reference List
Firebase, n.d. Build unit tests. [online] Available at: <https://firebase.google.com/docs/rules/unit-tests> [Accessed 2 October 2026].
Firebase, n.d. Test your Cloud Firestore Security Rules. [online] Available at: <https://firebase.google.com/docs/firestore/security/test-rules-emulator> [Accessed 2 October 2026].
IIE, 2026. INSY7315 Work Integrated Learning Module Manual 2026. The Independent Institute of Education (Pty) Ltd.
Node.js, n.d. Test runner. [online] Available at: <https://nodejs.org/api/test.html> [Accessed 2 October 2026].
*/
