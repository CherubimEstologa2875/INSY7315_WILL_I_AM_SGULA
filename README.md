# Sgula

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

## How to run

### Android application

1. Open the `INSY7315-APP` folder in Android Studio as the project root, not the repository root.
2. Wait for the Gradle sync to finish. The first sync takes longer than later ones because the
   wrapper downloads Gradle before it can build.
3. Pick a device or emulator running Android 15 (API 35) or higher and press Run.

**Signing in: authentication is a local prototype session rather than a live backend, so any
correctly formatted email address and any non-empty password will log you in.** Creating an account
works the same way and also sets the name shown on the profile screen. Choosing "Continue as
guest" locks the member-only screens until you log out from Settings.

### Website

1. Open `INSY7315-WEBSITE/INSY7315-WEBSITE.sln` in Visual Studio.
2. Press F5. The site builds and the browser opens on it automatically.

The Google Play button currently points at a placeholder URL because the app has not been
published.

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

