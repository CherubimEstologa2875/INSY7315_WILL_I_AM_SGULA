# Security & Privacy Implementation

## Main security controls

- Firebase Authentication is used to identify signed-in users.
- Firestore Security Rules control who can read and write data.
- Users can only access documents that belong to their own `userId`.
- Admin access is checked through the `Admin` collection.
- Normal users cannot give themselves an admin role.
- Users cannot change the owner of an existing document.
- Firestore fields, data types and lengths are validated before data is written.
- A default-deny rule blocks access to collections that do not have an explicit permission rule.

## User data protection

### Journal entries
- Only the user who created a journal entry can read, update or delete it.
- Admins do **not** have a special rule allowing them to read journal content.
- Journal fields are restricted to:
  - `userId`
  - `content`
  - `prompt`
  - `createdAt`
  - `updatedAt`
- Journal content is limited to 10,000 characters.
- Prompt is limited to 500 characters.
- Timestamps are validated using Firestore server time.

### Mood entries
- Users can only access their own mood entries.
- Mood level must be between 1 and 5.
- Notes are limited to 500 characters.
- Required fields and data types are checked.

### Quiz results
- Users can only access their own quiz results.
- Users cannot change the owner of a result.

### User profiles
- Users can manage their own profile.
- New profiles must have the `member` role.
- Normal users cannot change their role to `admin`.

## Admin security

Admins can manage authorised content such as:

- Audio content
- Broadcasts
- Daily broadcasts
- Relaxation tracks
- Quizzes
- Quiz questions
- Meditation categories

Normal users can read permitted content but cannot modify it.

## Points / succulent security

- Users cannot directly award themselves points.
- `SucculentState` writes are restricted.
- `SucculentPointAward` does not allow client writes.
- Point-awarding logic is handled by backend Cloud Functions.

## Default-deny rule

```text
match /{document=**} {
  allow read, write: if false;
}
```

If a new Firestore collection is added without an explicit rule, access is denied.

## Website security

Production website:

**https://sgula-task2-insy7315.web.app/**

- Firebase Hosting provides the public HTTPS address.
- Firebase Hosting forwards requests to the ASP.NET Core website running on Cloud Run.
- Cloud Run hosts the actual Sgula website.

## Production evidence

Firestore security rules were successfully deployed on **28 September 2026**:

```text
cloud.firestore: rules file firebase/firestore.rules compiled successfully
firestore: deployed indexes successfully
firestore: released rules firebase/firestore.rules to cloud.firestore

Deploy complete!
```

## Evidence to include

- Screenshot of `firebase/firestore.rules`
- Screenshot of `JournalEntry` rules
- Screenshot of `isAdmin()`
- Screenshot of default-deny rule
- Screenshot of successful Firestore deployment
- Screenshot of Firebase Console
- Screenshot of production website

**Note:** Only claim a runtime security test as PASS after actually performing the test.

## References

Firebase, n.d. *Control access to specific fields*. Available at: https://firebase.google.com/docs/firestore/security/rules-fields [Accessed 28 September 2026].

Firebase, n.d. *Securely query data*. Available at: https://firebase.google.com/docs/firestore/security/rules-query [Accessed 28 September 2026].

Firebase, n.d. *Writing conditions for Cloud Firestore Security Rules*. Available at: https://firebase.google.com/docs/firestore/security/rules-conditions [Accessed 28 September 2026].

Firebase, n.d. *Use Cloud Run with Firebase Hosting*. Available at: https://firebase.google.com/docs/hosting/cloud-run [Accessed 28 September 2026].

The Independent Institute of Education (IIE), 2026. *INSY7315 Work Integrated Learning Module Manual 2026*.
