# SnapMind Documentation

SnapMind is an Android app for managing screenshot and photo memories with OCR, image classification, auto-tagging, search, and memo workflows.

This documentation directory is the team source of truth for architecture, feature scope, implementation TODOs, progress tracking, and engineering decisions.

## Documentation Structure

| Path | Purpose |
| --- | --- |
| `architecture/` | System architecture, Android module design, AI pipeline, Room DB, Retrofit API contracts |
| `features/` | One implementation-ready specification per app feature |
| `todos/` | Master dashboard, team-specific TODOs, blockers, bug tracker |
| `progress/` | Daily logs, weekly reports, implementation history, milestone and feature status |
| `specs/` | Screen, navigation, schema, API, and model training specifications |

## Status Labels

Use the same labels across all docs:

| Label | Meaning |
| --- | --- |
| `Not Started` | No implementation work has begun |
| `In Progress` | Implementation is actively being worked on |
| `Completed` | Feature or task is implemented, tested, and merged |
| `Blocked` | Cannot proceed due to dependency, decision, bug, or missing asset |

## TODO Markers

Use these markers in all task lists:

- `[ ]` Not Started
- `[/]` In Progress
- `[x]` Completed
- `[!]` Blocked

## Feature Documents

Each feature spec must include:

- Goal
- User Scenario
- Functional Requirements
- Non-Functional Requirements
- UI Structure
- Processing Flow
- Data Flow
- Database Interaction
- API Interaction
- AI/OCR Logic
- Error Handling
- Edge Cases
- Dependencies
- TODO Checklist
- Current Status
- Progress Notes
- Future Improvements

## Update Rules

- Update feature TODOs before or during implementation.
- Update `todos/master-todo.md` when a feature status changes.
- Add implementation details to `progress/implementation-log.md` after meaningful code changes.
- Add day-level work notes to `progress/daily-log.md` during active development.
- Add bugs to `todos/bug-tracker.md` before fixing unless the bug is trivial and fixed immediately.
- Update `progress/milestone-status.md` at the end of each milestone review.

## Ownership

| Area | Primary Docs |
| --- | --- |
| Android UI | `specs/screen-specs.md`, `todos/frontend-todo.md` |
| ViewModel and Repository | `architecture/android-architecture.md`, feature specs |
| Room DB | `architecture/database-design.md`, `specs/room-schema.md`, `todos/backend-todo.md` |
| Retrofit API | `architecture/api-design.md`, `specs/retrofit-spec.md` |
| OCR and ML | `architecture/ai-pipeline.md`, `specs/ml-training-spec.md`, `todos/ai-todo.md` |
| Project Tracking | `todos/master-todo.md`, `progress/weekly-progress.md`, `progress/milestone-status.md` |
