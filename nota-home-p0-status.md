# Phase 0 — Search excludes locked notes

- Status: DONE
- Commit: fca0eac
- Changes: NoteDao search* queries now include `AND locked = 0`; explanatory comment; NoteDaoTest added.
- Tests: `:app:testDebugUnitTest` BUILD SUCCESSFUL (NoteDaoTest passed).
- Build: compileDebugKotlin success.
- New tests: 1
