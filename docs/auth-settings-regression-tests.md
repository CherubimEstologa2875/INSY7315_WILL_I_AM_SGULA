# Auth and settings regression test pack (Jira WIL-172)

This covers the two areas that sit underneath every other screen in the app, since a member cannot reach anything else if sign in is broken, and nothing they change in settings is worth much if it does not actually save. The pack is split the same way the rest of this project's testing is split, automated checks that run on a JVM without an emulator, and a manual list for whatever genuinely needs a device.

## Automated unit tests

These run with `./gradlew test`, no emulator required.

- `AuthErrorsTest` — every branch of `AuthErrors.signIn`, `register` and `passwordReset` is now exercised, not only the two or three cases the file started with. This includes the deactivated account message, the rate limit message, and confirming an unrecognised email and an unrecognised password look identical to the member, which is what stops the sign in screen being used to find out whether an email address has an account.
- `PasswordStrengthTest` — the weak, medium and strong bands the register screen shows live underneath the password field were pulled out of `RegisterFragment`'s text watcher into their own `PasswordStrength` object so they could be tested directly, covering the boundary between medium and strong at twelve characters and the cases where a password is long enough but missing a digit or a letter.
- `AuthValidationTest` — already existed, covers name, email and password field validation.

## Manual checklist

Nothing below has logic worth unit testing on its own, it is Fragment and Firestore wiring that only a real run can confirm.

- Register a new account, confirm the strength meter changes colour as you type, submit with a field left blank and confirm each field shows its own message, not a generic one.
- Log in with a correct password, then with a wrong one, and confirm the message does not say which part was wrong.
- Request a password reset for an email that does not have an account and confirm the screen still says it was sent, not that the account does not exist.
- Continue as a guest, confirm the launch screen's guest path works, then try to open mood, journal or the quiz and confirm each one redirects to the guest screen rather than crashing or showing empty data.
- Sign in as a member, open settings, flip each of the three switches, then sign out and back in and confirm all three kept their new value, since they are meant to follow the account to any device.
- Sign in as an admin, confirm the admin section appears in settings and a non-admin account does not see it.
- Have an admin deactivate a member's account from Manage client accounts, then try to sign in as that member and confirm `AuthErrors.DEACTIVATED` is what actually shows, not a generic failure.
- Log out as a member and confirm it returns to the launch screen with the session cleared, then do the same as a guest and confirm the button reads "Leave guest mode" rather than "Log out", matching what `SettingsFragment.showGuest` sets.
