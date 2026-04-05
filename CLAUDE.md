# CLAUDE.md — Environmental Monitoring Dashboard (COMP2850)

This file defines how Claude should operate in this repository. Read it fully before taking any action.

---

## Read this first — every session

Before doing anything else: check for `tasks/lessons.md` and apply any rules from it to this session.

---

## Project context

- **Course:** COMP2850 COIL (Collaborative Online International Learning)
- **System:** Environmental monitoring dashboard for livestock farm management
- **Environmental focus:** Soil moisture, water levels, air quality, temperature
- **Users:** Tom Hargreaves (farmer — needs simple at-a-glance status on mobile) and Priya Nair (field officer — needs multi-site overview, filters, CSV export)
- **Deadline:** 17:00 Friday 8 May | Demo: Monday 11 May

---

## Stack

- **Backend:** Kotlin + Ktor + Gradle · Exposed ORM · H2 (dev) / PostgreSQL (prod)
- **Frontend:** HTML / CSS / JavaScript (ES Modules) — served as static assets by Ktor from `backend/src/main/resources/static/`
- **Charts:** Chart.js (CDN)
- **Linting:** Detekt + KtLint
- **In-code docs:** KDoc on all public functions and classes
- **CI/CD:** GitHub Actions (`.github/workflows/ci.yml`)
- **Testing:** JUnit — unit + integration

---

## Directory structure

```
/
├── CLAUDE.md
├── README.md
├── SPRINT PLAN.md
├── docs/
│   ├── Personas.md
│   ├── wireframes/          # versioned — never delete old versions
│   └── diagrams/            # ERD and class diagrams — versioned
├── frontend/                # stub only — frontend lives inside Ktor
└── backend/
    ├── gradlew / build.gradle.kts / settings.gradle.kts
    ├── gradle/
    └── src/
        ├── main/kotlin/com/environmental/
        │   ├── Application.kt
        │   ├── Routing.kt
        │   ├── Serialization.kt
        │   ├── models/          # SensorReading, Site, AlertRule, AlertEvent, AlertSeverity
        │   ├── routes/          # ReadingsRoutes.kt, AlertsRoutes.kt
        │   ├── services/        # AlertEngine.kt, ValidationService.kt
        │   └── database/        # DatabaseConfig.kt, SensorReadingSchema.kt, AlertSchema.kt, SiteSchema.kt
        ├── main/resources/
        │   ├── application.yaml
        │   ├── logback.xml
        │   └── static/          # ← FRONTEND
        │       ├── index.html / trends.html / alerts.html / portal.html
        │       ├── css/style.css
        │       └── js/          # api.js, charts.js, main.js
        └── test/kotlin/com/environmental/
```

---

## Before writing any code

1. Find 2–3 existing patterns in the codebase that match what you're about to do.
2. Check the relevant model, schema, and route file before assuming field names or types.
3. If a task is ambiguous, state your interpretation in one sentence before acting.
4. If two valid interpretations produce meaningfully different implementations, surface both and ask.

---

## Execution model

- **Plan first** for any non-trivial task (3+ steps, architecture decisions, risky changes).
- If evidence changes assumptions mid-task, stop and re-plan.
- Fix bugs autonomously — reproduce, find root cause, apply the smallest safe fix.
- Keep changes simple and local; avoid unrelated refactors.
- Never mark a task done without proving it works.

---

## Scope discipline

- If implementing the task requires changing something outside its stated scope, stop.
- Surface to the user: "To do X I'd also need to change Y — should I?"
- Never expand scope silently, even if the change seems obviously correct.
- Improvements spotted during a task go in `tasks/todo.md`, not the current change.

---

## Engineering rules

- **Correctness over speed.** Prefer small diffs over rewrites.
- **No ghost code.** Don't create functions, classes, or modules that nothing uses yet.
- **No silent failures.** Every error path must be handled explicitly.
- **Backend validates everything.** Validate site IDs, sensor types, value ranges, timestamps server-side.
- **KDoc on all public functions and classes.** No exceptions.
- **Mark AI usage inline:** `// Used [model] to assist with X — lines Y–Z`

---

## Frontend rules

- All API calls go through `js/api.js` — no inline fetch calls in HTML files.
- All chart rendering goes through `js/charts.js` — no raw `new Chart()` calls in HTML files.
- Shared utilities go through `js/main.js`.
- Every page must handle three states explicitly: loading, empty, and error.
- Accessibility is non-optional: ARIA roles, skip links, keyboard navigation, contrast-checked colours.
- Do not hardcode colours or spacing inline — use CSS variables from `style.css`.

---

## Testing

- Write tests for: new routes, business logic, validation rules, error paths.
- Every new code path needs a test. No exceptions.
- Tests live in `backend/src/test/kotlin/com/environmental/` mirroring the `main/kotlin` structure.
- Test observable behaviour and contracts — not implementation details.

---

## Quality gates

```bash
cd backend
./gradlew test
./gradlew detekt
./gradlew ktlintCheck
```

All three must pass before work is considered done.

---

## Documentation rules

- If behaviour, config, or the API changes — update `README.md` in the same change.
- Wireframes and diagrams are versioned (`v1`, `v2`, `v3`) — never delete old versions.
- End every completion with explicit manual steps required (or "No manual steps required.").

---

## Error recovery

- If verification fails: diagnose root cause before retrying.
- If blocked after two attempts: stop, document what was tried, surface to user.
- Never silently skip a failing test.
- Never ask the user to verify something you can verify yourself.

---

## Learning loop

- Corrections are logged in `tasks/lessons.md`, not in this file.
- After any user correction: append the pattern with a concrete prevention rule.
- Apply the new rule immediately in the current session.

---
