package com.studybuddy.app.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/** A "reviewer" is a set of study notes belonging to a subject. */
@Entity
public class Reviewer {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long subjectId;
    public String title;
    public String content;
    public String summary;
    public long createdAt;

    public Reviewer() {
    }

    @Ignore
    public Reviewer(long subjectId, String title, String content) {
        this.subjectId = subjectId;
        this.title = title;
        this.content = content;
        this.summary = "";
        this.createdAt = System.currentTimeMillis();
    }
}
