# ParaTask

ParaTask is a local-first Android task manager focused on fast capture and flexible task organization. The product is inspired by Todoist's clarity while remaining an independent open-source application.

## Status

Versions `v0.1.0` through `v0.4.0` provide a persistent offline Inbox, Quick Add, completion with Undo, editable Task Details, Today, and the Upcoming week calendar. Development is currently focused on `v0.5`: projects, Browse, contextual task creation, and project assignment.

## Planned v1.0 scope

- Inbox, Today, Upcoming, and Browse
- tasks, projects, sections, labels, subtasks, reminders, and comments
- search, sorting, grouping, filters, and saved filters
- list, board, and calendar views
- smart task input
- offline Room database
- Material You, dynamic color, and dark mode

Accounts, cloud sync, collaboration, and non-Android clients are intentionally outside the `1.0` scope.

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- Android Gradle Plugin with built-in Kotlin support
- Room and Kotlin Coroutines (introduced with the `v0.1` data layer)
- Gradle version catalog
- GitHub Actions

## Build

Requirements:

- JDK 17
- Android SDK 36

```bash
./gradlew assembleDebug
```

Run all checks used by CI:

```bash
./gradlew lint testDebugUnitTest assembleDebug
```

## Architecture

The project uses unidirectional data flow and feature-oriented modules. See [the technical blueprint](docs/architecture/technical-blueprint.md) and the delivery plans for [v0.1](docs/development/v0.1-plan.md), [v0.2](docs/development/v0.2-plan.md), [v0.3](docs/development/v0.3-plan.md), [v0.4](docs/development/v0.4-plan.md), and [v0.5](docs/development/v0.5-plan.md).

## License

ParaTask is licensed under the [GNU General Public License v3.0](LICENSE).
