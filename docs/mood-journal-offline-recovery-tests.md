# Mood and journal offline and recovery test scenarios (Jira WIL-173)

Neither screen turns on any Firestore offline setting of its own, `setPersistenceEnabled` and `firestoreSettings` do not appear anywhere in the app's code, which means both run on the Android SDK's own default rather than anything this project configured, and that default is persistence turned on, unlike the web SDK where it is off unless a developer enables it. That default matters for what "offline" actually looks like here, a save made with no connection is queued to local disk and synced once the connection returns, it does not necessarily fail outright the moment the device goes offline. The scenarios below are written against that real behaviour rather than against a guess at what offline mode does.

`JournalErrors`, shared by both screens, is what turns a Firestore failure into the message a member actually reads. `JournalErrorsTest` is new and checks every branch the offline and recovery scenarios below depend on, `UNAVAILABLE` and `DEADLINE_EXCEEDED` both ask the member to check their connection, `PERMISSION_DENIED` and `UNAUTHENTICATED` both ask them to sign in again, and anything else falls back to a generic retry message rather than showing a raw exception.

## What a unit test can already confirm

- Every one of the five branches in `JournalErrors.message` returns the message it is supposed to, covered in `JournalErrorsTest`.
- A validation failure, for instance a journal entry over the ten thousand character limit, shows its own specific message rather than the generic fallback, since `WellnessValidation` throws `IllegalStateException` with the real reason and `JournalErrors` passes that message straight through instead of replacing it.

## What needs a device

Toggling real connectivity loss and recovery cannot be done from a JVM test, so these need the emulator's network controls or a physical device's airplane mode.

1. **Short offline save.** Turn on airplane mode, log a mood entry, and confirm it appears to save normally rather than showing an error, since persistence queues it locally. Turn airplane mode back off and confirm the entry actually reaches Firestore, not just the local cache, by checking it from a second device or the Firebase console.
2. **Offline journal entry survives a restart.** Same as above but for a journal entry, with the app force closed while still offline and reopened before reconnecting, to confirm the queued write is not lost when the process dies.
3. **Save fails outright.** This needs `UNAVAILABLE` or `DEADLINE_EXCEEDED` to actually reach the error callback rather than being queued, which is harder to force than a plain offline save. The more reliable way to see it is a very poor connection rather than a full outage, for instance a throttled network profile in the emulator's extended controls. Confirm the message on screen matches `JournalErrorsTest`'s coverage, check your internet connection and try again, and that the member's unsaved text stays in the editor rather than being cleared.
4. **Session expired mid session.** Disable the account from the admin side while the member is still signed in on a device, then have that member try to save a mood entry or a journal entry, and confirm `firestore.rules` rejects the write and the member sees the session expired message rather than a silent failure.
5. **Recovery does not duplicate.** After scenario 1 or 2, open mood or journal history while the device is back online and confirm the entry shows once, not twice, since a queued write that later succeeds should not be distinguishable from one that saved immediately.
