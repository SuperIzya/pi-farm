# AGENTS.md

## Project Overview

**Cooler** is an ESP32-S3 firmware that reads DHT22 temperature/humidity sensors and toggles a fan relay.

## Tech Stack

| Category | Technology |
| --- | --- |
| Language | C++ (Arduino framework) |
| Platform | ESP32-S3 via [PlatformIO](platformio.ini) |
| Board | Freenove ESP32-S3 WROOM |
| Key dependencies | DHT sensor library for ESPx, ArduinoJson (versions in [platformio.ini](platformio.ini)) |

## Build & Test

| Action | Command |
| --- | --- |
| Build | `pio run` |
| Upload to board | `pio run --target upload` |
| Serial monitor | `pio device monitor` |
| Run tests | `pio test` |
| Clean | `pio run --target clean` |

## Installation

PlatformIO CLI or VS Code PlatformIO extension required. Install via `pip install platformio` or the extension marketplace. See `docs/INSTALLATION.md` for full setup.

## Environment Setup

WiFi credentials are injected at build time via environment variables:

```sh
export WIFI_SSID="your_ssid"
export WIFI_PASSWORD="your_password"
```

Alternatively, edit `scripts/default_env.py` with defaults. See `docs/ENVIRONMENT.md` for details.

## Architecture Overview

Single-file firmware (`src/main.cpp`) using a generic `Sensor<T,R>` template (`src/Sensor.h`) to abstract sensor setup and JSON serialization. On each loop iteration, sensors are read, results printed as JSON over serial, and the fan relay is toggled. WiFi connects at startup for future network reporting. See `docs/ARCHITECTURE.md` for detailed data flow.

## Coding Standards

- Use Arduino/ESP-IDF idioms; prefer `static` free functions over class methods for simple helpers.
- Template-based abstractions for sensor types (`Sensor.h` pattern).
- Pin definitions as `#define` constants at file top.
- JSON serialization via ArduinoJson `JsonDocument`.

Full rules in `docs/STYLE.md`.

## Gotcha

- `WiFi.persistent(false)` is set intentionally — stale NVS flash credentials can override runtime values.
- The `scripts/default_env.py` file is git-tracked; do not commit real credentials there.
- DHT22 sensors need a 2-second minimum interval between reads (enforced by `delay(2000)` in loop).

See `docs/GOTCHA.md` for more.

---

## Updating This File

When you discover a new preference or gotcha:

```
"Please update AGENTS.md with [the new learning]"
```

When you change build commands, pin assignments, dependencies, or project structure, update the relevant sections of this file in the same commit.

## Plan Mode

- Make plans extremely concise.
- End with unresolved questions.

## Agent Responsibilities

1. **Clarify & investigate** — Ask questions, read code/docs
2. **Protect users** — Preserve behavior, handle edge cases
3. **Document & test** — Update docs and tests
4. **Keep scope tight** — Small, incremental changes

## Boundaries

- **Always do**: Build before committing (`pio run`), follow Arduino/ESP-IDF conventions, keep credentials out of source
- **Ask first**: New library dependencies, pin reassignments, WiFi/network protocol changes
- **Never do**: Commit WiFi credentials, skip build verification, force push to main

## Git & MR Workflows

- Commit format: `<type>: <description>` (types: `feat`, `fix`, `test`, `docs`, `refactor`)
- Run `pio run` before committing
- Keep commits small and focused on one logical change

**MR evidence checklist:**

- Build succeeds (`pio run`)
- No hardcoded credentials
- Brief description of intent and approach

See `docs/GIT_WORKFLOW.md` for full conventions.

---

_Derived from: platformio.ini, CMakeLists.txt, src/main.cpp, src/Sensor.h, scripts/wifi_credentials.py, scripts/default_env.py_
