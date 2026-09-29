# Git Branching Strategy

## Branches

### `main`
- Production branch.
- Contains the stable version of Sgula.
- Only production-ready changes should be merged here.

### `develop`
- Main development/integration branch.
- Completed features are merged here before being moved to `main`.

### `feature/*`
Used when developing new functionality.

Examples:
- `feature/audio-player`
- `feature/firebase-integration`
- `feature/user-profile`

### `bugfix/*`
Used when fixing existing functionality.

Examples:
- `bugfix/journal-validation`
- `bugfix/audio-loading`

---

## Development workflow

```text
feature/* / bugfix/*
        ↓
   Pull Request
        ↓
     develop
        ↓
   Testing / Review
        ↓
      main
        ↓
   Production