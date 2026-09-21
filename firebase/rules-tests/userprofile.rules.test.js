// rules tests for user profiles and roles, run against the emulator with npm install then npm test

import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, updateDoc } from "firebase/firestore";

const MEMBER = "member-uid";
const OTHER_MEMBER = "other-member-uid";
const ADMIN = "admin-uid";

let env;

const db = (uid) => (uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore());

const validProfile = (userId = MEMBER) => ({
  userId,
  email: "member@sgula.test",
  displayName: "Test Member",
  role: "member",
  remindersEnabled: true,
  sharesAnonymousInsights: true,
  activityProgressVisible: true,
});

before(async () => {
  env = await initializeTestEnvironment({
    // own project id so clearing data here does not affect the journal tests
    projectId: "demo-sgula-rules-profile",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  // seed an admin and two member profiles with the rules off
  await env.withSecurityRulesDisabled(async (context) => {
    const admin = context.firestore();
    await setDoc(doc(admin, "Admin", ADMIN), { adminId: ADMIN, name: "Anat", role: "admin" });
    await setDoc(doc(admin, "UserProfile", MEMBER), validProfile(MEMBER));
    await setDoc(doc(admin, "UserProfile", OTHER_MEMBER), validProfile(OTHER_MEMBER));
  });
});

describe("UserProfile", () => {
  test("a member can read and update their own profile", async () => {
    await assertSucceeds(getDoc(doc(db(MEMBER), "UserProfile", MEMBER)));
    await assertSucceeds(updateDoc(doc(db(MEMBER), "UserProfile", MEMBER), { remindersEnabled: false }));
  });

  test("a member cannot read another member's profile", async () => {
    await assertFails(getDoc(doc(db(MEMBER), "UserProfile", OTHER_MEMBER)));
  });

  test("a member cannot make themselves an admin", async () => {
    await assertFails(updateDoc(doc(db(MEMBER), "UserProfile", MEMBER), { role: "admin" }));
  });

  test("a member cannot register with an admin role", async () => {
    const newMember = "new-member-uid";
    await assertFails(
      setDoc(doc(db(newMember), "UserProfile", newMember), { ...validProfile(newMember), role: "admin" }),
    );
    await assertSucceeds(setDoc(doc(db(newMember), "UserProfile", newMember), validProfile(newMember)));
  });

  test("a member cannot create a profile under another uid", async () => {
    await assertFails(setDoc(doc(db(MEMBER), "UserProfile", "someone-else"), validProfile("someone-else")));
  });

  test("a member cannot move their profile to another owner", async () => {
    await assertFails(updateDoc(doc(db(MEMBER), "UserProfile", MEMBER), { userId: OTHER_MEMBER }));
  });

  test("an empty display name is rejected on create", async () => {
    const newMember = "blank-name-uid";
    await assertFails(
      setDoc(doc(db(newMember), "UserProfile", newMember), { ...validProfile(newMember), displayName: "" }),
    );
  });

  test("an admin can read a member profile", async () => {
    await assertSucceeds(getDoc(doc(db(ADMIN), "UserProfile", MEMBER)));
  });

  test("signed-out users cannot read a profile", async () => {
    await assertFails(getDoc(doc(db(null), "UserProfile", MEMBER)));
  });
});

describe("Admin collection", () => {
  test("a member cannot make themselves an admin by writing to Admin", async () => {
    await assertFails(setDoc(doc(db(MEMBER), "Admin", MEMBER), { adminId: MEMBER, name: "Sneaky", role: "admin" }));
  });

  test("a member cannot read the admin list", async () => {
    await assertFails(getDoc(doc(db(MEMBER), "Admin", ADMIN)));
  });
});
