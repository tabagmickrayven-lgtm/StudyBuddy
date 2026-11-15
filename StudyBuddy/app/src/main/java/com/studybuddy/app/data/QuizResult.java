package com.studybuddy.app.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class QuizResult {
    @PrimaryKey(autoGenerate = true)
    public long id;
    /** 0 means mixed subjects. */
    public long subjectId;
    public String mode;
    public int score;
    public int total;
    public int xp;
    public long timestamp;

    public QuizResult() {
    }

    @Ignore
    public QuizResult(long subjectId, String mode, int score, int total, int xp) {
        this.subjectId = subjectId;
        this.mode = mode;
        this.score = score;
        this.total = total;
        this.xp = xp;
        this.timestamp = System.currentTimeMillis();
    }
}
