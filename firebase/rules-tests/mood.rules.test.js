// Security rules tests for mood tracking modeled after journal.rules.test.js
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
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  where,
} from "firebase/firestore";

const MEMBER = "member-uid";
const OTHER_MEMBER = "other-member-uid";
const ADMIN = "admin-uid";
const ENTRY_ID = "mood-1";

let env;

const db = (uid) =>
  uid
    ? env.authenticatedContext(uid).firestore()
    : env.unauthenticatedContext().firestore();

const validEntry = (userId = MEMBER) => ({
  userId,
  moodLevel: 4,
  note: "Slept well for once.",
  createdAt: serverTimestamp(),
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
    await setDoc(doc(admin, "MoodEntry", ENTRY_ID), {
      userId: MEMBER,
      moodLevel: 3,
      note: "Private note",
      createdAt: Timestamp.now(),
    });
  });
});

describe("members can use their own mood log", () => {
  test("a member can save a valid mood", async () => {
    await assertSucceeds(
      addDoc(collection(db(MEMBER), "MoodEntry"), validEntry()),
    );
  });

  test("a member can save a mood without a note", async () => {
    await assertSucceeds(
      addDoc(collection(db(MEMBER), "MoodEntry"), {
        ...validEntry(),
        note: "",
      }),
    );
  });

  test("a member can read their own mood", async () => {
    await assertSucceeds(getDoc(doc(db(MEMBER), "MoodEntry", ENTRY_ID)));
  });

  test("a member can list their own moods", async () => {
    const own = query(
      collection(db(MEMBER), "MoodEntry"),
      where("userId", "==", MEMBER),
    );
    await assertSucceeds(getDocs(own));
  });

  test("a member can change the mood on their own entry", async () => {
    await assertSucceeds(
      updateDoc(doc(db(MEMBER), "MoodEntry", ENTRY_ID), { moodLevel: 5 }),
    );
  });
});

describe("moods are saved securely", () => {
  test("cannot save a mood for someone else", async () => {
    await assertFails(
      addDoc(collection(db(MEMBER), "MoodEntry"), validEntry(OTHER_MEMBER)),
    );
  });

  test("signed-out users cannot save moods", async () => {
    await assertFails(addDoc(collection(db(null), "MoodEntry"), validEntry()));
  });

  test("mood level must be between 1 and 5", async () => {
    await assertFails(
      addDoc(collection(db(MEMBER), "MoodEntry"), {
        ...validEntry(),
        moodLevel: 0,
      }),
    );
    await assertFails(
      addDoc(collection(db(MEMBER), "MoodEntry"), {
        ...validEntry(),
        moodLevel: 6,
      }),
    );
  });

  test("mood level must be a whole number", async () => {
    await assertFails(
      addDoc(collection(db(MEMBER), "MoodEntry"), {
        ...validEntry(),
        moodLevel: 3.5,
      }),
    );
  });

  test("cannot save a note longer than 500 characters", async () => {
    await assertFails(
      addDoc(collection(db(MEMBER), "MoodEntry"), {
        ...validEntry(),
        note: "a".repeat(501),
      }),
    );
  });

  test("cannot backdate a mood with a client timestamp", async () => {
    const backdated = {
      ...validEntry(),
      createdAt: Timestamp.fromDate(new Date("2020-01-01")),
    };
    await assertFails(addDoc(collection(db(MEMBER), "MoodEntry"), backdated));
  });

  test("cannot save a mood without a timestamp", async () => {
    const { createdAt, ...missing } = validEntry();
    await assertFails(addDoc(collection(db(MEMBER), "MoodEntry"), missing));
  });

  test("cannot give yourself points through extra fields", async () => {
    await assertFails(
      addDoc(collection(db(MEMBER), "MoodEntry"), {
        ...validEntry(),
        awardedPoints: 100,
      }),
    );
  });

  test("cannot move a mood to another user", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "MoodEntry", ENTRY_ID), {
        userId: OTHER_MEMBER,
      }),
    );
  });

  test("cannot change when a mood was logged", async () => {
    await assertFails(
      updateDoc(doc(db(MEMBER), "MoodEntry", ENTRY_ID), {
        createdAt: Timestamp.fromDate(new Date("2020-01-01")),
      }),
    );
  });
});

describe("admins cannot see a member's moods", () => {
  test("an admin cannot read a member's mood", async () => {
    await assertFails(getDoc(doc(db(ADMIN), "MoodEntry", ENTRY_ID)));
  });

  test("an admin cannot query a member's moods", async () => {
    const memberMoods = query(
      collection(db(ADMIN), "MoodEntry"),
      where("userId", "==", MEMBER),
    );
    await assertFails(getDocs(memberMoods));
  });

  test("an admin cannot list every mood", async () => {
    await assertFails(getDocs(collection(db(ADMIN), "MoodEntry")));
  });

  test("an admin cannot edit a member's mood", async () => {
    await assertFails(
      updateDoc(doc(db(ADMIN), "MoodEntry", ENTRY_ID), { moodLevel: 1 }),
    );
  });

  test("an admin cannot delete a member's mood", async () => {
    await assertFails(deleteDoc(doc(db(ADMIN), "MoodEntry", ENTRY_ID)));
  });
});

describe("other people cannot see a member's moods", () => {
  test("another member cannot read the mood", async () => {
    await assertFails(getDoc(doc(db(OTHER_MEMBER), "MoodEntry", ENTRY_ID)));
  });

  test("another member cannot query it", async () => {
    const memberMoods = query(
      collection(db(OTHER_MEMBER), "MoodEntry"),
      where("userId", "==", MEMBER),
    );
    await assertFails(getDocs(memberMoods));
  });

  test("signed-out users cannot read it", async () => {
    await assertFails(getDoc(doc(db(null), "MoodEntry", ENTRY_ID)));
  });
});

/* Reference List
1. Firebase. n.d. Build unit tests. [Online]. Available at: https://firebase.google.com/docs/rules/unit-tests [Accessed 23 September 2026].
2. Firebase. n.d. Test your Cloud Firestore Security Rules. [Online]. Available at: https://firebase.google.com/docs/firestore/security/test-rules-emulator [Accessed 23 September 2026].
3. Node.js. n.d. Test runner. [Online]. Available at: https://nodejs.org/api/test.html [Accessed 23 September 2026].
*/
