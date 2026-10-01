# Technical decisions

This document keeps a running log of the real technical decisions made whilst building Task 2, together with the reasoning behind each one, so that both the presentation and the final report have something genuine to draw from rather than needing to reconstruct the thinking afterward.

## Branching strategy over enforced branch protection

Date: 20 September 2026

We wanted `main` locked so nobody could push to it directly, only merge through a pull request. Checked the repository permissions and found that none of the five group members have admin access on the GitHub repository, only lecturer and institution accounts do. GitHub branch protection rules need admin access to configure, so it cannot be set up by any of us.

Instead we documented the same discipline as a convention in the README: `main` stays production only, `develop` is the integration branch everyone merges into first, and `feature/*` and `bugfix/*` name new branches. It relies on the group actually following it rather than GitHub enforcing it, but it is the only option available given the permission level we were given on the repository.

## Design patterns used in the audio player

Date: 22 September 2026

The audio and broadcast screens follow the same two patterns the Task 1 documentation already named for the rest of the app, they are just easy to miss without pointing them out directly.

`FirebaseWellnessRepository` is a Repository pattern. Every screen, mood, journal, audio, broadcast, goes through the same class for reads and writes instead of calling Firestore directly, so the screens do not need to know anything about collection names, document shapes, or query structure.

`PlayerFragment`'s use of `Player.Listener` is an Observer pattern. The ExoPlayer instance is the subject, the fragment is the observer reacting to `onPlaybackStateChanged`, `onIsPlayingChanged`, and `onPlayerError` without the player needing to know anything about the UI it is driving.

## Where the built audio player differs from the Task 1 design document

Date: 22 September 2026

Checked the real implementation against the Task 1 documentation's AudioSession State Diagram. Three real differences turned up, each one a deliberate choice made along the way rather than something overlooked.

The documentation describes writing playback progress to Firestore every five seconds while a track plays. What actually got built only writes one document, when the session completes, which matches the Jira ticket's own wording exactly, record completed listening sessions, so progress tracking throughout the track was dropped in favour of that simpler behaviour.

The documentation gives a live broadcast an 80 percent completion threshold and an on demand track 100 percent. That distinction does not exist anywhere in what got built. The cloud function that awards points pays a flat 20 points either way, and the Broadcast data model does not even have a field for whether something is live. Everything in the app today is a static file the user either finishes or does not.

The documentation also describes offline caching and resuming playback from the last position. Neither is in any Task 2 Jira ticket, so neither got built. This one was simply never in scope for Task 2 to begin with, so there was nothing here to simplify in the first place.

One actual bug did come out of this check, fixed the same day. ExoPlayer plays automatically the moment it finishes buffering unless told otherwise, so the player was starting playback before the user had pressed play at all. Set playWhenReady to false so buffering still happens in the background but playback now waits for an actual tap, matching the documentation's Create state, which waits for the user to request the stream before buffering begins in earnest.

## Error feedback consolidated onto the pattern mood already established

Date: 23 September 2026

Mood tracking merged into develop with two shared pieces already built and reused across journal and mood, JournalErrors for turning raw Firestore exceptions into calm member facing messages, and SgulaModal for a success confirmation dialog. Audio previously had neither for errors, it only logged failures with no message shown to the user.

Wired all four audio screens to JournalErrors instead of hardcoded error strings, and fixed a real bug found while doing it, PlayerFragment saved the audio session without ever catching the exception WellnessValidation can throw, unlike mood's save function which already guards against this exact risk.

## Success feedback stayed a toast, not a SgulaModal, after checking the Task 1 documentation

Date: 23 September 2026

For completion feedback there were two real options, keep audio's lightweight toast, which fits a passive event like a track finishing, or switch to SgulaModal to match mood's save confirmation exactly. First implemented SgulaModal, prioritising one consistent success pattern across every screen over a context specific difference, since the rubric rewards visual and interaction consistency.

Checked that choice against the Task 1 documentation's own Work Breakdown Structure before finalising it. Mood's task explicitly says "save confirmation" as a deliverable, journal's does too, word for word. Audio's task says "+20 points on completion" only, no mention of confirmation anywhere. That lines up with the same distinction between a deliberate save and a passive completion argued above, already present in the original design document from the start, rather than something invented afterwards to justify the choice.

Also checked whether any other screen actually has a live points toast to be consistent with. None do. Journal, quiz, and succulent all use the same PointsToast style in their layouts, but none of them are wired to any Kotlin code, they are unused Task 1 mockup markup. Audio's original toast was the only real, working implementation of that pattern anywhere in the app.

Reverted to the toast. It matches what the original documentation specifically asked for on this screen, and it does not conflict with any other screen's actual, live behaviour, since none exists to conflict with.

## An unreviewed push to develop regressed real work, and the fix was built rather than just reported

Date: 26 September 2026

A commit was pushed directly onto `develop` without going through a branch or a pull request, rewriting `PlayerFragment` from ExoPlayer over to `MediaPlayer`. The reason given was that audio was not playing at all and was not being fetched from Firebase, which is a believable claim, since nothing within this environment was ever able to run the app in order to confirm or disprove it either way. Whilst fixing that specific problem, the rewrite also removed the sleep timer, the buffering indicator, the rotation lock, the retry on a playback failure, and the fix that only showed the points toast once the Firestore save had actually succeeded rather than showing it regardless.

Instead of raising this and waiting for those pieces to be put back, the decision taken was to build the restoration directly on its own branch, whilst leaving the new `MediaPlayer` architecture and the admin upload work from that same commit completely untouched. Every missing piece was added back on top of what already existed, together with one further fix that neither version had before, a fast double tap on play or pause during the buffering window could call `start()` or `pause()` on a `MediaPlayer` that had not yet finished preparing, which throws. A small `isPreparing` guard was added to close that gap.

This incident matters less for the one file it affected and more for what it demonstrates about why the branching convention decided on earlier exists in the first place, since a direct push onto `develop` is precisely the failure mode that convention is meant to prevent, and this is the real cost when it happens regardless, working code gets silently lost instead of being reviewed and merged on purpose.

## Extending the rules test convention already in the repository to cover audio sessions and the guest role

Date: 26 to 30 September 2026

Security rules tests for journal, mood, quiz, and user profiles already existed within `firebase/rules-tests`, built on Node's own test runner together with `@firebase/rules-unit-testing` against a local emulator. Nothing covered `AudioSession` ownership or the guest experience specifically, whilst both of these sit squarely under the Testing and Quality Assurance tickets.

Two new files were added following that same existing pattern rather than inventing a different one, `audioSession.rules.test.js` for ownership, the admin override, and the one case that is genuinely security relevant, a member being unable to award themselves points by editing `pointsAwarded` directly on their own session, and `guestAccess.rules.test.js` for what a signed out user can and cannot reach. This environment has no Java, whilst the Firestore emulator needs it, so these tests could not be executed in the usual sense. What could be done instead was running `node --test` directly against the real installed packages with no emulator running, which resolved every import correctly and got exactly as far as the expected host and port of the firestore emulator must be specified error, confirming that the files are structurally sound right up to the one dependency this environment does not have.

## The dummy data sweep missed partial hardcoding because it only checked whether a file had any Firebase call at all

Date: 30 September 2026

An earlier pass through every screen for leftover Task 1 placeholder content used the number of repository calls as the signal, zero calls meant the screen was worth a closer look, whilst anything above zero was treated as probably fine. That heuristic missed a real problem. `HomeFragment` had exactly one Firebase call, for the daily affirmation, whilst the member's name, their streak, their succulent stage and points, and a daily goal ring were all still hardcoded straight into the layout. The same thing was found on the Settings screen's streak history card once it was pointed out there too.

Fixing the succulent card and the greeting was straightforward, since `SucculentFragment` already had the correct stage and points logic and `HomeFragment` simply needed to call the same repository method. The streak and the daily goal card were a different problem entirely, since neither concept exists anywhere within the app or the Cloud Functions, not a single field, not a single computation. Building either from scratch would have meant inventing new product scope under what was meant to be a dummy data ticket, so both were removed outright rather than faked with a second layer of placeholder content. The same reasoning was applied again a few hours later when a similar since date on the Succulent screen turned out to have real `createdAtMillis` data sitting behind it after all, and was wired up properly instead of being removed.

## A red X on every branch's checks turned out to be a billing problem, not a code problem

Date: 1 October 2026

GitHub's branch overview showed a failing check, zero out of two or three, on every single branch at once, `main`, `develop`, and every feature branch, not only the one with new changes sitting on it. That same pattern appearing across branches that share no commits in common rules out anything in the code itself being the actual cause. Checking the real run output directly rather than guessing at it confirmed this, since both the Android and the Website workflows carried the identical annotation, the job was not started because recent account payments have failed or the spending limit needs to be increased. This is an account level billing problem on the GitHub organisation itself, not something that can be fixed from within the repository, and it is worth flagging to whoever holds admin access before it gets mistaken for a genuinely broken pipeline during the presentation.
