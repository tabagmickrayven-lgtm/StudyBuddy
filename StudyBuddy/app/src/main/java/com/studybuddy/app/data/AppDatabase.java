package com.studybuddy.app.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** The app's built-in SQLite database (via Room). Lives fully on the device. */
@Database(
        entities = {Subject.class, Reviewer.class, Question.class, QuizResult.class, ChatMessage.class},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract StudyDao dao();

    private static volatile AppDatabase instance;

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(), AppDatabase.class, "studybuddy.db").build();
                }
            }
        }
        return instance;
    }
}
