// Security rules tests for the private journal (WIL-37, WIL-38, WIL-44), run with
// @firebase/rules-unit-testing against the emulator (Firebase, n.d.) and Node's test runner (Node.js, n.d.).
// Run from this folder:  npm install  then  npm test
// Needs Java 11+ for the Firestore emulator. Nothing here touches the live project.

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
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  where,
} from "firebase/firestore";

const MEMBER = "member-uid";
const OTHER_MEMBER = "other-member-uid";
const ADMIN = "admin-uid";
const ENTRY_ID = "entry-1";

let env;

const db = (uid) => (uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore());

const validEntry = (userId = MEMBER) => ({
  userId,
  content: "The quiet stretch after lunch felt calm.",
  prompt: "What made you feel calm today?",
  createdAt: serverTimestamp(),
  updatedAt: serverTimestamp(),
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
  // Seed an admin account and one journal entry owned by MEMBER, bypassing the rules.
  await env.withSecurityRulesDisabled(async (context) => {
    const admin = context.firestore();
    await setDoc(doc(admin, "Admin", ADMIN), { name: "Practice admin", role: "admin" });
    await setDoc(doc(admin, "JournalEntry", ENTRY_ID), {
      userId: MEMBER,
      content: "Private reflection",
      prompt: "What made you feel calm today?",
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now(),
    });
  });
});

describe("members can use their own journal", () => {
  test("a member can save a valid entry with server timestamps", async () => {
    await assertSucceeds(addDoc(collection(db(MEMBER), "JournalEntry"), validEntry()));
  });

  test("a member can read their own entry", async () => {
    await assertSucceeds(getDoc(doc(db(MEMBER), "JournalEntry", ENTRY_ID)));
  });

  test("a member can list their own entries", async () => {
    const own = query(collection(db(MEMBER), "JournalEntry"), where("userId", "==", MEMBER));
    await assertSucceeds(getDocs(own));
  });

  test("a member can edit their entry without changing owner or created time", async () => {
    await assertSucceeds(
      updateDoc(doc(db(MEMBER), "JournalEntry", ENTRY_ID), { content: "Edited", updatedAt: serverTimestamp() }),
    );
  });
});

describe("entries are saved securely (WIL-37, WIL-38)", () => {
  test("cannot save an entry for someone else", async () => {
    await assertFails(addDoc(collection(db(MEMBER), "JournalEntry"), validEntry(OTHER_MEMBER)));
  });

  test("signed-out users cannot save entries", async () => {
    await assertFails(addDoc(collection(db(null), "JournalEntry"), validEntry()));
  });

  test("cannot backdate an entry with a client timestamp", async () => {
    const backdated = { ...validEntry(), createdAt: Timestamp.fromDate(new Date("2020-01-01")) };
    await assertFails(addDoc(collection(db(MEMBER), "JournalEntry"), backdated));
  });

  test("cannot save an entry without a timestamp", async () => {
    const { createdAt, ...missing } = validEntry();
    await assertFails(addDoc(collection(db(MEMBER), "JournalEntry"), missing));
  });

  test("cannot save an empty entry", async () => {
    await assertFails(addDoc(collection(db(MEMBER), "JournalEntry"), { ...validEntry(), content: "" }));
  });

  test("cannot save an entry longer than 10,000 characters", async () => {
    await assertFails(addDoc(collection(db(MEMBER), "JournalEntry"), { ...validEntry(), content: "a".repeat(10001) }));
  });

  test("cannot add unexpected fields", async () => {
    await assertFails(addDoc(collection(db(MEMBER), "JournalEntry"), { ...validEntry(), sharedWithTherapist: true }));
  });

  test("cannot move an entry to another user", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "JournalEntry", ENTRY_ID), { userId: OTHER_MEMBER, updatedAt: serverTimestamp() }),
    );
  });
});

describe("journal content cannot be accessed by admins (WIL-44)", () => {
  test("an admin cannot read a member's entry", async () => {
    await assertFails(getDoc(doc(db(ADMIN), "JournalEntry", ENTRY_ID)));
  });

  test("an admin cannot query a member's entries", async () => {
    const memberEntries = query(collection(db(ADMIN), "JournalEntry"), where("userId", "==", MEMBER));
    await assertFails(getDocs(memberEntries));
  });

  test("an admin cannot list every journal entry", async () => {
    await assertFails(getDocs(collection(db(ADMIN), "JournalEntry")));
  });

  test("an admin cannot edit a member's entry", async () => {
    await assertFails(
      updateDoc(doc(db(ADMIN), "JournalEntry", ENTRY_ID), { content: "changed", updatedAt: serverTimestamp() }),
    );
  });

  test("an admin cannot delete a member's entry", async () => {
    await assertFails(deleteDoc(doc(db(ADMIN), "JournalEntry", ENTRY_ID)));
  });

  test("an admin cannot write entries on a member's behalf", async () => {
    await assertFails(addDoc(collection(db(ADMIN), "JournalEntry"), validEntry(MEMBER)));
  });

  test("an admin can still read the member's succulent progress", async () => {
    await env.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "SucculentState", MEMBER), { userId: MEMBER, totalPoints: 10 });
    });
    await assertSucceeds(getDoc(doc(db(ADMIN), "SucculentState", MEMBER)));
  });
});

describe("other people cannot read a member's journal", () => {
  test("another member cannot read the entry", async () => {
    await assertFails(getDoc(doc(db(OTHER_MEMBER), "JournalEntry", ENTRY_ID)));
  });

  test("another member cannot query it", async () => {
    const memberEntries = query(collection(db(OTHER_MEMBER), "JournalEntry"), where("userId", "==", MEMBER));
    await assertFails(getDocs(memberEntries));
  });

  test("signed-out users cannot read it", async () => {
    await assertFails(getDoc(doc(db(null), "JournalEntry", ENTRY_ID)));
  });
});

/* Reference List
IIE, 2026. INSY7315 Work Integrated Learning Module Manual 2026. The Independent Institute of Education (Pty) Ltd.
Firebase, n.d.. Build unit tests. [online] Available at: <https://firebase.google.com/docs/rules/unit-tests> [Accessed 21 September 2026].
Firebase, n.d.. Test your Cloud Firestore Security Rules. [online] Available at: <https://firebase.google.com/docs/firestore/security/test-rules-emulator> [Accessed 21 September 2026].
Node.js, n.d.. Test runner. [online] Available at: <https://nodejs.org/api/test.html> [Accessed 21 September 2026].
*/
