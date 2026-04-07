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

### Gradle Modules

The project is split into three Gradle modules (defined in `settings.gradle.kts`):

```
:shared          # KMP library — platform-agnostic business logic, models, data layer
:composeApp      # KMP application — Compose Multiplatform UI + platform entry points
:server          # JVM-only — Ktor sync server
```

### `:shared` — Common Business Logic

The core of the app. A Kotlin Multiplatform **library** that compiles to Android, JVM, JS, and Wasm. Contains everything that is not UI and not a native entry point: domain models, repositories, use cases, database definitions, networking client, and `expect/actual` platform abstractions.

| Source set | Responsibility |
|---|---|
| `commonMain` | Domain models (`FocusSession`, `User`, `Tag`, `AppWhitelist`), repository interfaces, use cases, `expect` declarations for `PlatformMonitor`, Ktor client setup, SQLDelight schema, shared constants |
| `androidMain` | `actual` implementations — `UsageStatsManager`-based `PlatformMonitor`, Android-specific data sources |
| `jvmMain` | `actual` implementations — JNA-based active-window polling (`PlatformMonitor`) for Windows/macOS/Linux |
| `jsMain` / `wasmJsMain` | `actual` stubs for web targets (monitoring is not applicable on web) |

**Package:** `cz.aaa.unit2026` · **Depends on:** nothing (leaf module)

### `:composeApp` — UI & Platform Entry Points

The user-facing application. A Kotlin Multiplatform **application** that depends on `:shared` and renders the UI with Compose Multiplatform. Each platform source set contains only the thin entry point and any platform-specific UI affordances (e.g., Android overlays, desktop window chrome).

| Source set | Responsibility |
|---|---|
| `commonMain` | All shared Compose UI — screens, components, Material 3 theme, navigation, ViewModels / state holders. This is where the timer ring, settings screens, session reports, and blocking overlay composables live. No platform imports allowed here. |
| `androidMain` | `MainActivity` (thin lifecycle host), Android manifest, launcher resources, `AccessibilityService` registration, overlay permission handling |
| `jvmMain` | Desktop `main()` — creates the application `Window`, configures desktop-specific distribution (DMG/MSI/Deb) |
| `webMain` | Web `main()` — `ComposeViewport` entry, `index.html` shell, CSS |

**Package:** `cz.aaa.unit2026` · **Depends on:** `:shared`

### `:server` — Sync Server

A standalone JVM application (not multiplatform). Runs a lightweight Ktor server on port `SERVER_PORT` (8080) that synchronizes focus sessions between Android and Desktop clients. Stateless where possible — sessions are owned locally and synced, not streamed live.

**Package:** `cz.aaa.unit2026` · **Depends on:** `:shared`

### Layer Boundaries (strictly enforced)

```
:composeApp / commonMain  →  Compose UI (renders state, emits events)
        ↓ user actions
:shared / commonMain      →  State Holders (ViewModel / MVI presenter)
        ↓ collects from
:shared / commonMain      →  Domain (use cases — only when logic is complex/reused)
        ↓ delegates to
:shared / commonMain      →  Repository (owns SSOT, coordinates local + remote)
        ↓ uses
:shared / commonMain      →  Data Sources: LocalDB (SQLDelight) | KtorClient | PlatformMonitor (expect)
        ↓ platform boundary
:shared / androidMain     →  UsageStatsManager, Android-specific data sources
:shared / jvmMain         →  JNA + Win32/macOS/Linux active window polling
```

### Key Shared Models (:shared / commonMain)

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
