# Sgula — INSY7315 Task 2

INSY7315: Information Systems 3E, Work Integrated Learning

Group Name: WILL I AM

## Team members

Group 1

| # | Name | Student number |
|---|------|----------------|
| 1 | Mpho Molefe | ST10317078 |
| 2 | Ophec Funis | ST10089492 |
| 3 | Divan Fourie | ST10434242 |
| 4 | David Botha | ST10446408 |
| 5 | Cherubim Estologa | ST10443277 |

## Repository structure

| Path | Purpose |
|---|---|
| `INSY7315-APP` | Kotlin Android application for members, guests and administrators. |
| `INSY7315-WEBSITE` | .NET 8 ASP.NET Core MVC marketing website. |
| `functions` | TypeScript Firebase Cloud Functions and seed data. |
| `firebase` | Firestore security rules and composite indexes. |
| `docs/architecture.md` | Final solution architecture and request flows. |
| `docs/database-design.md` | Firestore collections, fields, relationships and access rules. |

## How to run

**Production website:** https://sgula-task2-insy7315.web.app/

### Prerequisites

- Android Studio with an Android 15 (API 35) or newer emulator/device.
- JDK 21 (the Gradle wrapper is used by the Android project).
- .NET 8 SDK for the marketing website.
- Node.js 20 and the Firebase CLI for the backend.
- Access to the Firebase project `sgula-task2-insy7315`.

### Firebase/backend

The Android app uses Firebase Authentication, Cloud Firestore, Cloud Storage and callable
Cloud Functions. Firebase configuration is intentionally not committed. Obtain the project's
`google-services.json` and save it as `INSY7315-APP/app/google-services.json` before building the
Android app.

From the repository root:

```powershell
cd functions
npm ci
npm run build
cd ..
firebase emulators:start --only functions,firestore
```

The emulator UI is available at `http://127.0.0.1:4000` when the Firebase CLI starts it. To
deploy rules, indexes and functions to the configured project:

```powershell
firebase login
firebase use sgula-task2-insy7315
firebase deploy --only functions,firestore:rules,firestore:indexes
```

The quiz seed writes categories and questions to the configured Firebase project. Authenticate
Application Default Credentials first, then run it from `functions`:

```powershell
gcloud auth application-default login
cd functions
npm run seed
```


### Android application

1. Open the `INSY7315-APP` folder in Android Studio as the project root, not the repository root.
2. Wait for the Gradle sync to finish. The first sync takes longer than later ones because the
   wrapper downloads Gradle before it can build.
3. Pick a device or emulator running Android 15 (API 35) or higher and press Run.

**Signing in:** registered users authenticate through Firebase Authentication. Creating an account
also creates the matching `UserProfile/{uid}` document. Choosing "Continue as guest" allows public
content while member-only screens remain locked until the user signs in.

### Website

1. Open `INSY7315-WEBSITE/INSY7315-WEBSITE.sln` in Visual Studio.
2. Press F5. The site builds and the browser opens on it automatically.

The Google Play button currently points at a placeholder URL because the app has not been
published.

### Website from the command line

```powershell
dotnet run --project INSY7315-WEBSITE/INSY7315-WEBSITE/INSY7315-WEBSITE.csproj
```

The website is an informative ASP.NET Core MVC site. It does not read or write Firebase data.

## Architecture and database design

The final architecture diagram and the main runtime flows are documented in
[docs/architecture.md](docs/architecture.md). The Firestore collection design, relationships,
indexes and security boundaries are documented in [docs/database-design.md](docs/database-design.md).

## Making a user an admin

Admin access requires both the Firebase Admin document and the admin role on the user's profile.

1. Open the Firebase Console and select the Firebase project connected to the app.
2. Go to **Authentication → Users** and find the user you want to make an admin.
3. Copy the user's **User UID**.
4. Go to **Firestore Database → Data → Admin**. If the `Admin` collection does not exist, create it.
5. Create a new document in the `Admin` collection and use the user's **Firebase UID as the document ID**. You can add a field such as `role: "admin"`.
6. Go to the **UserProfile** collection and open the document with the same UID.
7. Change the user's `role` from `member` to `admin`.
8. Have the user sign out and sign back into the app so the new admin role is picked up.

The UID must match in both collections:

```text
Admin/{USER_UID}

UserProfile/{USER_UID}
    role: "admin"
```

## About

Sgula is a wellbeing companion built for the clients of Sgula Growth & Development, a therapy
practice run by Anat Casey. Therapy happens in scheduled sessions, but most of the work happens in
the days between them. Sgula gives that stretch a structure without turning it into another thing
to keep up with.

This repository holds two projects.

**INSY7315-APP** is the Android application, written in Kotlin. It is the product itself. Clients
get a short audio broadcast recorded by the therapist each morning, a library of white noise,
nature and guided meditation with a sleep timer, a one to five mood check-in with optional notes, a
private journal with a daily prompt, and a one question wellness quiz that recommends a meditation
matched to how the day feels. Every activity earns points that grow a virtual succulent from seed
through sprout, growing and blooming, which wilts after seven days of inactivity and recovers as
soon as anything is logged. Guests can open the app and listen without an account, but journalling,
mood logging and the succulent stay locked until they register. The practice has its own admin
screens for uploading and scheduling audio, managing client accounts and viewing engagement, with a
deliberate boundary: administrators can see streaks and completed sessions, never journal entries.

**INSY7315-WEBSITE** is the supporting marketing site, an ASP.NET Core MVC web application. It is
informative only and does not reimplement any part of the app. It is a single page that explains
what Sgula does, shows real screenshots of the app, breaks down how the succulent scores activity,
sets out the privacy boundaries, and links to the Google Play listing.

## Branching

`main` is production only. Nothing gets pushed to it directly, changes land through a pull
request from `develop`.

`develop` is where finished feature branches get merged first. This is what everyone should branch
off for new work.

Branch names:

- `feature/short-description` for new functionality, for example `feature/audio-player`
- `bugfix/short-description` for fixing something that already exists

Open a pull request into `develop` once a branch is ready. Merge `develop` into `main` only when a
batch of finished work is ready to be treated as done.

## Pull request and review process

1. Once a team member has finished working on a branch, we first pull `develop` into it to get any changes.
2. We check that the app still builds and runs on an emulator or Android device.
3. We open a pull request into `develop`, say what it changes, and which Jira tickets it covers.
4. Another team member reads over the changes. Anything that needs fixing goes in a comment on the
   pull request, and the author pushes the fix to the same branch.
5. Once it has been read over and the GitHub Actions checks pass, it gets merged into `develop`.

## Continuous integration

GitHub Actions runs the workflows in `.github/workflows` on every pull request and push to `develop`
or `main`.

- `android.yml` builds the app and runs the unit tests.
- `website.yml` builds the website.

`google-services.json` is not committed, so the Android workflow reads it from a repository secret
called `GOOGLE_SERVICES_JSON`. It is set under **Settings → Secrets and variables → Actions**, and
its value is the whole contents of `INSY7315-APP/app/google-services.json`.

