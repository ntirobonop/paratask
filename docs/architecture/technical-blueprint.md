# ParaTask technical blueprint

## Goals

The codebase must support incremental delivery from a small offline Inbox to a complete local task manager without making the UI depend on database details. Each increment is specified before production code changes.

## Platform baseline

| Item | Decision |
| --- | --- |
| Language | Kotlin 2.4.20 (AGP built-in Kotlin) |
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.0 |
| Compile / target SDK | 36 |
| Minimum SDK | 23 |
| UI | Jetpack Compose BOM 2026.06.01, Material 3 1.4.0 |
| Persistence | Room 2.8.5, introduced in the v0.1 data slice |
| Concurrency | Kotlin Coroutines and Flow |
| Java toolchain | JDK 17 |

Stable dependencies are preferred. Alpha APIs require a documented product need and an explicit architecture decision.

## Modules

The foundation started with only modules that contained real code:

```text
app
├── core:designsystem
└── feature:inbox
    └── core:model
```

The v0.1 persistence slice through the v0.5 Projects slice add:

```text
core:database → core:model
core:data     → core:database + core:model
core:ui       → core:model
feature:task  → core:model + core:data + core:ui
feature:inbox → core:model + core:data + core:ui
feature:today → core:model + core:data + core:ui
feature:upcoming → core:model + core:data + core:ui
feature:projects → core:model + core:data + core:ui
```

Later features remain isolated under `feature:*`. Features do not depend directly on other feature modules; navigation and shared contracts are promoted to `core` only when a concrete second consumer appears.

## Package convention

The root namespace is `io.github.ntirobonop.paratask`.

```text
io.github.ntirobonop.paratask
├── core.model
├── core.database
├── core.data
├── core.designsystem
├── core.ui
└── feature.<feature-name>
```

## Data flow

```text
Room DAO → repository implementation → ViewModel → immutable UI state → Compose UI
```

- UI never accesses a DAO.
- Room entities and domain models are separate types connected by explicit mappers.
- ViewModels expose immutable `StateFlow` values and accept user intents through methods.
- Time and ID creation are injectable at repository boundaries to keep tests deterministic.

## Domain model

`TaskId` is a value class instead of a plain `String`. Equivalent ID types are introduced with their entities (`ProjectId`, `SectionId`, and others).

The initial `Task` contains future-facing nullable relationship and scheduling fields. UI code uses only capabilities delivered in the current increment.

## Room schema v2

Schema v1 creates `tasks`:

| Column | SQLite type | Nullable | Notes |
| --- | --- | --- | --- |
| `id` | TEXT | no | primary key, UUID |
| `title` | TEXT | no | non-blank domain invariant |
| `description` | TEXT | no | defaults to empty string |
| `due_date` | TEXT | yes | ISO local date |
| `due_time` | TEXT | yes | ISO local time |
| `project_id` | TEXT | yes | relationship activated with Projects |
| `section_id` | TEXT | yes | relationship activated with Sections |
| `parent_task_id` | TEXT | yes | relationship activated with Subtasks |
| `is_completed` | INTEGER | no | boolean |
| `created_at` | INTEGER | no | epoch milliseconds |
| `updated_at` | INTEGER | no | epoch milliseconds |
| `completed_at` | INTEGER | yes | epoch milliseconds |
| `deleted_at` | INTEGER | yes | soft deletion timestamp |
| `sort_order` | INTEGER | no | manual ordering |

Schema v2 adds `projects` with name, curated color and icon values, archive/delete state, timestamps, and manual order. It rebuilds `tasks` with a foreign key from `project_id` to `projects.id` using `ON DELETE SET NULL`. The explicit `MIGRATION_1_2` preserves every task and normalizes any unexpected orphan assignment to Inbox.

Room schema JSON is exported and committed from schema version 1 onward. Every schema change requires a migration and a migration test; destructive fallback is not allowed in production.

## Repository API through v0.5

```kotlin
interface TaskRepository {
    fun observeInbox(): Flow<List<Task>>
    fun observeToday(date: LocalDate): Flow<List<Task>>
    fun observeTasksInDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Task>>
    fun observeProjectTasks(projectId: ProjectId): Flow<List<Task>>
    fun observeTask(id: TaskId): Flow<Task?>
    suspend fun createTask(
        title: String,
        description: String = "",
        dueDate: LocalDate? = null,
        projectId: ProjectId? = null,
    ): TaskId
    suspend fun updateTask(task: Task)
    suspend fun setCompleted(id: TaskId, completed: Boolean)
    suspend fun setDeleted(id: TaskId, deleted: Boolean)
}
```

`ProjectRepository` owns project creation, editing, archive/restore, soft deletion, and manual reordering. Project deletion clears task and future section assignments in the same Room transaction, so tasks return to Inbox instead of being deleted. Task assignment accepts only active projects.

This focused API is expanded into query objects when sorting, grouping, and filters arrive. A speculative query engine is intentionally not part of v0.5. Upcoming observes one inclusive week range and derives per-day lists and indicators in UI state.

## Navigation through v0.5

The application has Inbox, Today, Upcoming, and Browse top-level destinations plus project and task-detail destinations. The app layer records the originating task-list destination so Back from Task Details returns to Inbox, Today, Upcoming, or the correct project. Feature modules remain independent and communicate through callbacks.

A larger navigation framework remains deferred until deep links or more deeply nested destinations create a concrete need for it. The current state-based navigation keeps the increment small while preserving feature boundaries.

## Shared task-list UI in v0.3

Inbox and Today are concrete consumers of the same task row, empty/loading container, bottom navigation, date control, and composer. These components live in `core:ui`; screen-specific state, events, copy, and contextual defaults stay inside their feature modules. This prevents duplicated behavior without making one feature depend on another.

Task dates use `LocalDate` throughout the domain and repository. Room keeps the existing nullable ISO-8601 `due_date` column, so schema version 1 remains valid and v0.3 needs no migration. The current calendar date is injected at the Today boundary for deterministic tests.

Upcoming uses ISO Monday-to-Sunday weeks. Its selected date is transient ViewModel state, while a single Room range observation supplies the visible week's tasks. This avoids seven parallel database flows and keeps task indicators reactive. The current local date is injected at the feature boundary for deterministic week navigation tests.

Projects use `ProjectId` and `ProjectIcon` domain types. Browse exposes active and archived projects separately; only active projects appear in Task Composer and Task Details selectors. A nullable `Task.projectId` continues to represent Inbox, so assignment changes use the same task model in every feature.

## Testing strategy

- `core:model`: invariants and pure domain behavior.
- `core:database`: DAO tests and committed Room schema.
- `core:data`: repository behavior with deterministic time and IDs.
- `feature:*`: ViewModel tests and focused Compose semantics tests.
- `app`: navigation and critical end-to-end workflows.

CI runs lint, JVM unit tests, and a debug build for every push and pull request. Instrumented tests are added when Room and user workflows require an Android runtime.

## Architecture decisions

Material architectural changes must be recorded in `docs/architecture/decisions/` with context, decision, consequences, and alternatives. The blueprint is updated in the same pull request as the code.
