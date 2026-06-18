# allauth-client-kotlin

This package is the Kotlin/Android companion to `allauth-client-swift` for Django AllAuth headless authentication.

Keep implementation work scoped to this repository. Do not edit the root workspace or the Android app repo when changing this library unless Gabe explicitly asks.

Preserve parity with the Swift client where practical:

- AllAuth responses remain dynamic JSON maps with convenience accessors.
- JWT access tokens are memory-only.
- Session tokens are persisted through the configured token storage.
- Refresh tokens are handled through a secure storage abstraction supplied by the consuming app.
- A 401 response retries once after JWT refresh when a refresh token exists.
- Expired or rejected refresh tokens clear local JWT state while leaving room for session-token recovery.

Do not hard-code secrets or machine-specific paths in code, tests, or documentation.

