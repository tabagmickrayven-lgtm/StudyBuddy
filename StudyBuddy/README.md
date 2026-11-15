# StudyBuddy 📚🤖

An all-in-one study app for Android, written in **Java**, built with **Android Studio**.
Everything runs on the phone: the database, the AI, the quizzes. No internet and no account needed.

## Features

| Tab | What it does |
|---|---|
| **Home** | Level, XP bar, daily streak, progress stats, recent quizzes, one-tap Daily Challenge |
| **Library** | Reviewers (study notes) grouped by subject. Add your own notes and the AI summarizes them and generates quiz questions |
| **Play** | Classic Quiz, Speed Round (15 s timer + bonus XP), Flashcards (missed cards come back) |
| **AI Teacher** | Chat with an on-device tutor: explains topics from your notes, summarizes reviewers, quizzes you, tells you what to study next, gives study tips |

Playful touches: XP, levels, streaks, answer combos (🔥), shake / pulse animations, level-up messages.

## How the built-in AI works

`app/src/main/java/com/studybuddy/app/ai/LocalAI.java` is a fully offline NLP engine:

- **TF-IDF retrieval**: finds the most relevant sentences across all your reviewers to answer questions.
- **Extractive summarization**: scores sentences by word frequency and keeps the best ones.
- **Question generation**: turns a sentence into a fill-in-the-blank multiple-choice question and builds
  distractors from other key terms (and nearby numbers for years / counts).
- **Performance coach**: reads your quiz history and points you at your weakest subject.

It is classic, lightweight AI. It does not generate free-form text like a large language model.
To upgrade the teacher to an LLM later, replace `LocalAI.reply(...)` with a call to an on-device model
(for example MediaPipe LLM Inference or llama.cpp). Nothing else in the app needs to change.

## Built-in database

Room (SQLite) with five tables: `Subject`, `Reviewer`, `Question`, `QuizResult`, `ChatMessage`.
Created automatically on first launch and seeded with 4 subjects, 5 reviewers and ~45 questions
(`data/Seeder.java`). Your notes, questions, results and chat history are all stored locally.

## Open and run

1. Install **Android Studio** (Koala or newer) with **JDK 17** (bundled).
2. `File > Open` and select the `StudyBuddy` folder. Let Gradle sync (needs internet once to download dependencies).
3. Pick an emulator or a phone with USB debugging, then press **Run ▶**.

Requirements: minSdk 24 (Android 7.0), compileSdk 34, Android Gradle Plugin 8.5.2, Gradle 8.7.

## Project layout

```
app/src/main/java/com/studybuddy/app/
  MainActivity.java        bottom navigation + first-launch seeding
  ai/LocalAI.java          the built-in AI engine
  data/                    Room entities, DAO, database, Seeder
  ui/                      Home, Library, Play, Teacher fragments + Quiz, Flashcard, Reviewer, AddReviewer activities
  util/                    Async (background work), Prefs (XP / streak)
app/src/main/res/          layouts, theme, icons, menu
```

## Ideas for next steps

- Spaced-repetition scheduling for flashcards
- Import notes from PDF / photos (ML Kit text recognition)
- Home-screen widget with the daily streak
- Dark theme
