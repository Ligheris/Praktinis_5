# NotesApp

An Android notes application (`com.mipt.notes`) written in Java that supports switching between two local storage backends—SQLite and JSON file storage—and synchronizing notes between them.

## Features

- **Create, Edit & Delete Notes**: Manage notes with a title, content, and last-updated timestamp (`MainActivity`, `NoteEditorActivity`, `NoteAdapter`).
- **Input Validation**: Ensures note title and content are non-empty before saving (`NoteValidator`).
- **Dual Storage Backends**:
  - **SQLite (Primary)**: Persists notes in a local `notes.db` SQLite database (`SqliteNoteRepository`).
  - **File (Secondary)**: Persists notes in `notes_storage.json` inside internal app storage (`FileNoteRepository`).
  - Active storage selection is saved in `SharedPreferences` (`notes_storage_prefs`).
- **Storage Synchronization** (`NoteStorageManager`):
  - **Primary (SQLite) → Secondary (File)**: Overwrites file storage with SQLite notes.
  - **Secondary (File) → Primary (SQLite)**: Overwrites SQLite with file storage notes.
  - **Two-Way Merge**: Merges notes from both storages by ID, keeping the newest version based on `updatedAt`, and writes the result to both storages.
- **Localization**: Supports English (`res/values/strings.xml`) and Lithuanian (`res/values-lt/strings.xml`).

## Tech Stack & Requirements

- **Language**: Java 17
- **Build System**: Gradle (AGP `9.4.1`)
- **SDK Versions**: `minSdk 24`, `targetSdk 34`, `compileSdk 36`
- **Libraries**: AndroidX AppCompat, Material Components, Activity KTX, ConstraintLayout, RecyclerView
- **Testing**: JUnit 4 (unit tests), AndroidX Test JUnit & Espresso (instrumented tests)

## Project Structure

```text
app/src/
├── main/
│   ├── java/com/mipt/notes/
│   │   ├── model/Note.java
│   │   ├── storage/
│   │   │   ├── NoteRepository.java
│   │   │   ├── SqliteNoteRepository.java
│   │   │   ├── FileNoteRepository.java
│   │   │   └── NoteStorageManager.java
│   │   ├── ui/
│   │   │   ├── MainActivity.java
│   │   │   ├── NoteEditorActivity.java
│   │   │   └── NoteAdapter.java
│   │   └── validation/NoteValidator.java
│   └── res/
└── test/
    └── java/com/mipt/notes/
        ├── storage/NoteStorageManagerTest.java
        └── validation/NoteValidatorTest.java
```

## Build & Test

Run unit tests:

```bash
./gradlew test
```

Build debug APK:

```bash
./gradlew assembleDebug
```
