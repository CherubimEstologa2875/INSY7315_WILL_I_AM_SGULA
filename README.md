# Sgula — INSY7315 Final Project

INSY7315: Information Systems 3E, Work Integrated Learning

Group Name: WILL I AM

## Assessment access

These are the links and access details a marker needs first:

| Item | Details |
|---|---|
| Public GitHub repository | https://github.com/CherubimEstologa2875/INSY7315_WILL_I_AM_SGULA |
| Deployed website | https://sgula-task2-insy7315.web.app/ |
| Firebase project | `sgula-task2-insy7315` |
| Android app build | Open `INSY7315-APP` in Android Studio and follow the [user guide](docs/user-guide.md). |
| User guide | [docs/user-guide.md](docs/user-guide.md) |
| Architecture and database design | [docs/architecture.md](docs/architecture.md) and [docs/database-design.md](docs/database-design.md) |

### Marker credentials

Current accounts:
#### Admin
admin@gmail.com
Password1

#### User
user@gmail.com
Password1

## Presentation video
https://youtu.be/woksBAVAJdY 

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
| `docs/user-guide.md` | Marker-facing guide for guest, member and administrator use and local setup. |

## Technology stack

- Android application: Kotlin, Android SDK 36, Gradle, AndroidX, Material Components and Media3.
- Website: ASP.NET Core MVC on .NET 8, containerised with Docker and deployed to Google Cloud Run.
- Backend: Firebase Authentication, Cloud Firestore, Cloud Storage and TypeScript Cloud Functions.
- Quality and delivery: Gradle unit tests, Node test suites, Firestore rules tests and GitHub Actions.

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
Cloud Functions. The project's `google-services.json` is intentionally committed at
`INSY7315-APP/app/google-services.json` so that markers can build and run the debug app straight
away, without having to request or set up Firebase configuration first.

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

For the complete marker workflow, including guest/member/admin actions and validation checks, see
[the user guide](docs/user-guide.md).

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
private journal with a daily prompt, and a five question wellness quiz that recommends a meditation
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

## Audio credits

None of the audio in the app is ours. The licence for each group of recordings is set out below,
and every track is listed in the reference list at the end of this section. Each reference points
at the page the file came from rather than the download link, so the reference keeps working.

### White noise and nature — Pixabay

Seven tracks found through the Pixabay nature white-noise search and used under the Pixabay
Content License (Pixabay, n.d.): free to use, including commercially, with no attribution
required. We credit the uploaders anyway. Three of them were originally posted on Freesound and
appear on Pixabay under the freesound_community account. The year in each reference is the date
the track was published on Pixabay.

### Guided meditation — UCLA Mindful Awareness Research Center

Three recordings, copyright The Regents of the University of California and used under
CC BY-NC-ND 4.0 (Creative Commons, n.d.). Non-commercial use only, and they are uploaded whole
because the licence does not allow edited versions. The practice would need its own recordings
before the app could ever be sold or carry advertising.

### Radio station recordings — not licensed for use

Tracks taken from a third-party upload of the Grand Theft Auto III radio stations (Rockstar Games,
2001). This is commercial game and record label music with no licence for reuse, so it is listed
here for completeness only and is being removed from Firebase Storage.

### Broadcasts

Broadcasts are recorded by the practice itself and uploaded through the admin broadcast screen, so
no outside licence applies to them.

### Reference list

Cosmic-Scapes, 2025. Soft brown noise. [sound recording] Pixabay. Available at: <https://pixabay.com/sound-effects/soft-brown-noise-299934/> [Accessed 2 October 2026].

Creative Commons, n.d. Deed - Attribution-NonCommercial-NoDerivatives 4.0 International. [online] Available at: <https://creativecommons.org/licenses/by-nc-nd/4.0/> [Accessed 2 October 2026].

extrasounds, 2025. White noise. [sound recording] Pixabay. Available at: <https://pixabay.com/sound-effects/white-noise-358382/> [Accessed 2 October 2026].

freesound_community, 2022. Howling wind. [sound recording] Originally recorded by JSilverSound. Pixabay. Available at: <https://pixabay.com/sound-effects/howling-wind-14892/> [Accessed 2 October 2026].

freesound_community, 2022. Sea wind noise. [sound recording] Originally recorded by Jose_DanielMs. Pixabay. Available at: <https://pixabay.com/sound-effects/sea-wind-noise-26809/> [Accessed 2 October 2026].

freesound_community, 2022. Underwater white noise. [sound recording] Originally recorded by psiboy123. Pixabay. Available at: <https://pixabay.com/sound-effects/underwater-white-noise-46423/> [Accessed 2 October 2026].

Pixabay, n.d. Content License. [online] Available at: <https://pixabay.com/service/license-summary/> [Accessed 2 October 2026].

RecordedReality, 2023. Heavy rain white noise. [sound recording] Pixabay. Available at: <https://pixabay.com/sound-effects/heavy-rain-white-noise-159772/> [Accessed 2 October 2026].

Rockstar Games, 2001. Double Clef FM. [sound recording] Grand Theft Auto III radio stations. Uploaded to the Internet Archive by Jared1799, 2021. Available at: <https://archive.org/details/GTAIII-RadioStations-2001> [Accessed 2 October 2026].

Rockstar Games, 2001. Flashback 95.6. [sound recording] Grand Theft Auto III radio stations. Uploaded to the Internet Archive by Jared1799, 2021. Available at: <https://archive.org/details/GTAIII-RadioStations-2001> [Accessed 2 October 2026].

Rockstar Games, 2001. Game FM. [sound recording] Grand Theft Auto III radio stations. Uploaded to the Internet Archive by Jared1799, 2021. Available at: <https://archive.org/details/GTAIII-RadioStations-2001> [Accessed 2 October 2026].

UCLA Mindful Awareness Research Center, 2024. Body and sound meditation. [sound recording] Los Angeles: UCLA Health. Available at: <https://www.uclahealth.org/programs/marc/free-guided-meditations/guided-meditations> [Accessed 2 October 2026].

UCLA Mindful Awareness Research Center, 2024. Body scan meditation. [sound recording] Los Angeles: UCLA Health. Available at: <https://www.uclahealth.org/programs/marc/free-guided-meditations/guided-meditations> [Accessed 2 October 2026].

UCLA Mindful Awareness Research Center, 2024. Breathing meditation. [sound recording] Los Angeles: UCLA Health. Available at: <https://www.uclahealth.org/programs/marc/free-guided-meditations/guided-meditations> [Accessed 2 October 2026].

WhiteNoiseSleepers, 2024. Rainy day in town with birds singing. [sound recording] Pixabay. Available at: <https://pixabay.com/sound-effects/rainy-day-in-town-with-birds-singing-194011/> [Accessed 2 October 2026].

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

`google-services.json` is committed with the app, but the Android workflow also reads it from a
repository secret called `GOOGLE_SERVICES_JSON`. It is set under **Settings → Secrets and variables → Actions**, and
its value is the whole contents of `INSY7315-APP/app/google-services.json`.

