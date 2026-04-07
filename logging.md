# Active Window / Foreground App Tracking — Research Log

A running log of approaches investigated for detecting the currently focused app/window on each platform. Used to inform the `ForegroundAppMonitor` implementation.

---

## Linux — KDE Plasma 6 on Wayland

**Environment:** KWin 6.6.3, Wayland session, KDE Plasma

### Approach 1: KWin Scripting + journald ✅ WORKS

Load a KWin JavaScript via D-Bus `/Scripting`, connect to `workspace.windowActivated`, emit via `print()` which lands in journald. Tail journald from the JVM side.

**Available fields per event:**
| Field | KWin property | Example |
|---|---|---|
| App identifier | `window.resourceClass` | `firefox`, `org.kde.konsole` |
| Window title | `window.caption` | `"YouTube — Mozilla Firefox"` |
| Process ID | `window.pid` | `2116` |

**Script:**
```javascript
workspace.windowActivated.connect(function(window) {
    if (window) {
        print("ACTIVE_WINDOW:" + window.caption + "|" + window.resourceClass + "|" + window.pid);
    }
});
// Fire immediately for current window on load
var w = workspace.activeWindow;
if (w) print("ACTIVE_WINDOW:" + w.caption + "|" + w.resourceClass + "|" + w.pid);
```

**Load/start via shell:**
```bash
qdbus org.kde.KWin /Scripting org.kde.kwin.Scripting.loadScript /path/to/script.js plugin_name
qdbus org.kde.KWin /Scripting org.kde.kwin.Scripting.start
```

**Tail output from JVM:**
```bash
journalctl -f -n 0 _COMM=kwin_wayland
# filter lines starting with ACTIVE_WINDOW:
```

**Notes:**
- `windowActivated` fires reliably on every focus change including browser tab title changes
- `caption` includes document/tab title — useful for detecting YouTube, etc. inside Firefox without network interception
- Script survives across virtual desktop switches
- Script must be unloaded on app exit: `qdbus org.kde.KWin /Scripting org.kde.kwin.Scripting.unloadScript plugin_name`
- Script ID `0` returned on load is normal — not an error

**Limitations:**
- journald as IPC is scrappy; long-term replace with a D-Bus service the script calls back into
- Requires `qdbus` on the system (part of `qt6-tools` / `kde-cli-tools`, present on any KDE install)

---

### Approach 2: KWin D-Bus `queryWindowInfo` ❌ BLOCKED

`qdbus org.kde.KWin /KWin org.kde.KWin.queryWindowInfo` — waits for the user to click a window interactively. Not usable for automated polling.

---

### Approach 3: KWin D-Bus passive signals ❌ NO RELEVANT SIGNALS

Monitored `dbus-monitor --session "type='signal',sender='org.kde.KWin'"` — KWin does not emit window focus change signals on the session bus. No subscribable event.

---

### Approach 4: `xdotool` ❌ NOT INSTALLED / X11 ONLY

`xdotool getactivewindow` — not installed, and would only see XWayland apps anyway. Misses native Wayland windows entirely.

---

### Approach 5: X11 `XGetInputFocus` via JNA ❌ WAYLAND

Dead on Wayland sessions. Would only work for XWayland-hosted apps.

---

## Linux — Other Compositors (not yet tested)

| Compositor | Likely approach | Status |
|---|---|---|
| Hyprland | `hyprctl activewindow -j` or Hyprland IPC socket | Not tested |
| Sway | `swaymsg -t get_tree` or `$SWAYSOCK` i3 IPC | Not tested |
| GNOME Wayland | No public API without extension — nearly locked down | Not tested |

---

## Android (planned)

| Approach | Notes |
|---|---|
| `UsageStatsManager` | Requires `PACKAGE_USAGE_STATS` permission. Polling-based (query time range). Returns package name only — no window title. |
| `AccessibilityService` | Event-driven focus changes. More power, more user permission friction. |

---

## Desktop — Windows / macOS (planned)

| Platform | API | Notes |
|---|---|---|
| Windows | JNA → `GetForegroundWindow` + `QueryFullProcessImageName` | Returns exe name + window title |
| macOS | JNA → `NSWorkspace.frontmostApplication` | Returns bundle ID + app name |
