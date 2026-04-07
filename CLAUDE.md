# OpenJetTracks

A rigorous, cross-platform productivity tool that forces users to stay on task. Actively monitors the user's environment across PC and Android, detects distracting apps/websites, and intervenes to keep them focused.

---

## Project Overview

Unlike passive Pomodoro timers, this app polls OS-level APIs to detect unauthorized app usage and intervenes — either via overlay blocking or force-closing the app. Built with Kotlin Multiplatform (KMP) to maximize code sharing while retaining native OS-level performance.

**Targets:** Android + Desktop (JVM, Windows/macOS/Linux)

---

## MVP Features

- **Unified Timer State** — start a session on PC, Android enters focus mode instantly via sync server
- **Active Distraction Monitoring** — polls OS for active window/foreground app (no screen recording)
- **Intervention** — full-screen blocking overlay or force-close unauthorized apps
- **Offline Resilience** — sessions tracked locally, synced when connection is available
- **Strict Mode** — configurable; prevents usage of non-whitelisted apps entirely
- **Light/Dark Mode**
- **Localization** — Esperanto and Emoji/Japanese language support
- **Reports** — post-session review showing whether focus was respected

---

## Architecture

### Module Structure

```
commonMain/         # Shared business logic — timer state machine, models, sync, DB
ui-shared/          # Compose Multiplatform UI (buttons, timer rings, settings screens)
androidMain/        # Android native layer — UsageStatsManager, overlays, AccessibilityService
desktopMain/        # Desktop JVM layer — JNA + Win32/macOS/Linux active window polling
server/             # Lightweight Ktor sync server
```

### Layer Boundaries (strictly enforced)

```
UI (Compose MP)
  ↓ user actions
State Holders (ViewModel / MVI presenter in commonMain)
  ↓ collects from
Domain (use cases — only when logic is complex/reused)
  ↓ delegates to
Repository (commonMain — owns SSOT, coordinates local + remote)
  ↓ uses
Data Sources: LocalDB (SQLDelight) | KtorClient | PlatformMonitor (expect/actual)
  ↓ platform boundary
androidMain / desktopMain native implementations
```

### Key Shared Models (commonMain)

- `FocusSession` — running/paused/finished state + timestamps
- `User` — identity/config
- `Tag` — session categorization
- `AppWhitelist` — per-platform allowed apps/sites during strict mode

---

## KMP Architectural Rules

### Source Sets

- `commonMain` must **never** reference platform APIs (Android SDK, JNA, Java AWT, etc.)
- Platform behavior is isolated via `expect/actual` — keep `actual` surfaces minimal and implementation-focused; no business logic in `actual` declarations
- Intermediate source sets (e.g. `jvmMain` shared between Android + Desktop if applicable) require explicit justification

### State Management

- All screens with meaningful complexity require a state holder (ViewModel or shared presenter)
- Expose a **single immutable `UiState` flow** — no leaking `MutableStateFlow` to UI
- One-time effects (navigation, snackbar) are modeled separately from persistent `UiState`
- No business logic in `@Composable` functions — composables render state and emit events only

### Data Layer

- `Repository` is the **single source of truth** for each data type — UI and state holders never bypass it
- Repositories coordinate local (SQLDelight) and remote (Ktor) sources; callers are insulated from the details
- Network DTOs are mapped at the data-layer boundary — they must not leak into domain or UI layers
- Offline-first: local DB is the source of truth; network syncs to it

### Android Entry-Point Discipline

- `Activity`/`Fragment` are thin lifecycle-bound UI hosts — no business logic
- `AccessibilityService` / `Service` for monitoring and overlays must delegate immediately to the shared layer; they are not architecture containers
- `UsageStatsManager` polling lives in `androidMain` behind a `PlatformMonitor` interface defined in `commonMain`

### Desktop Layer

- JNA-based window polling lives in `desktopMain` behind the same `PlatformMonitor` interface
- No Win32/macOS/Linux types in shared code

### Failure Handling

- A consistent failure model is used across all layers — do not use raw strings as error types
- Failures propagate through the repository boundary as typed results
- User-facing error messages are assembled at the presentation edge only

### Security

- Strict mode enforcement happens in the native layer — the overlay/blocking decision is not trusted to UI state alone
- The whitelist configuration is owned by the repository; UI reads it, does not own it
- No sensitive session data logged unnecessarily

---

## Tech Stack

| Concern | Library |
|---|---|
| UI | Compose Multiplatform |
| Async / State | Coroutines + Flow |
| Networking | Ktor Client (shared) + Ktor Server |
| Local DB | SQLDelight |
| DI | Koin (KMP-compatible) |
| Desktop OS APIs | JNA |
| Android OS APIs | UsageStatsManager, AccessibilityService |

---

## Server

Lightweight Ktor server for session synchronization between Android and Desktop clients. Stateless where possible; sessions are owned locally and synced, not streamed live.

---

## What to Avoid

- Business logic in `@Composable` functions, `Activity`, `Service`, or `BroadcastReceiver`
- Android SDK types in `commonMain`
- Repositories bypassed by state holders or UI
- DTOs used as domain or UI models
- Mutable state exposed outside a state holder
- Platform-only logic that could live in `commonMain` behind an interface, but doesn't
- Logging sensitive session or user data
