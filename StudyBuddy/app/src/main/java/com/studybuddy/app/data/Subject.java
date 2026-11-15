package com.studybuddy.app.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Subject {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String emoji;

    public Subject() {
    }

    @Ignore
    public Subject(String name, String emoji) {
        this.name = name;
        this.emoji = emoji;
    }
}
