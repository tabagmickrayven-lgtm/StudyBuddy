package com.studybuddy.app.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public boolean fromUser;
    public String text;
    public long timestamp;

    public ChatMessage() {
    }

    @Ignore
    public ChatMessage(boolean fromUser, String text) {
        this.fromUser = fromUser;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }
}
