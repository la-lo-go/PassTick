# Contributing

PassTick is a private, owner-maintained project. Open an issue before you start a large change.

Read [AGENTS.md](https://github.com/la-lo-go/PassTick/blob/main/AGENTS.md) for the architecture contracts, persistent decisions, and required validation.

Keep each change small and focused. Add tests for behavior changes.

Base your branch on `main`.

## Validation

Run the checks before you request a review:

```powershell
.\gradlew.bat :android:testDebugUnitTest :android:lintDebug :android:assembleRelease
```

Run instrumented and Compose tests on API 29 and API 37 when emulators are available. Test a release on a physical Android 10 or later device before publication.
