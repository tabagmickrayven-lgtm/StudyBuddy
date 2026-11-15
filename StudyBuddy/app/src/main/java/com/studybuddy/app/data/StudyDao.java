package com.studybuddy.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StudyDao {

    // ---- Subjects ----
    @Insert
    long insertSubject(Subject s);

    @Query("SELECT * FROM Subject ORDER BY name")
    List<Subject> subjects();

    @Query("SELECT COUNT(*) FROM Subject")
    int subjectCount();

    @Query("SELECT * FROM Subject WHERE id = :id")
    Subject subject(long id);

    // ---- Reviewers ----
    @Insert
    long insertReviewer(Reviewer r);

    @Query("SELECT * FROM Reviewer ORDER BY createdAt DESC")
    List<Reviewer> reviewers();

    @Query("SELECT * FROM Reviewer WHERE id = :id")
    Reviewer reviewer(long id);

    @Query("UPDATE Reviewer SET summary = :summary WHERE id = :id")
    void setSummary(long id, String summary);

    @Query("DELETE FROM Reviewer WHERE id = :id")
    void deleteReviewerById(long id);

    // ---- Questions ----
    @Insert
    void insertQuestions(List<Question> questions);

    @Insert
    long insertQuestion(Question q);

    @Query("SELECT * FROM Question WHERE (:sid = 0 OR subjectId = :sid) ORDER BY RANDOM() LIMIT :n")
    List<Question> randomQuestions(long sid, int n);

    @Query("SELECT * FROM Question WHERE reviewerId = :rid ORDER BY RANDOM() LIMIT :n")
    List<Question> randomForReviewer(long rid, int n);

    @Query("SELECT COUNT(*) FROM Question")
    int questionCount();

    @Query("SELECT COUNT(*) FROM Question WHERE reviewerId = :rid")
    int questionCountFor(long rid);

    @Query("DELETE FROM Question WHERE reviewerId = :rid")
    void deleteQuestionsFor(long rid);

    @Query("DELETE FROM Question WHERE reviewerId = :rid AND source = 1")
    void deleteAiQuestionsFor(long rid);

    // ---- Results ----
    @Insert
    void insertResult(QuizResult r);

    @Query("SELECT * FROM QuizResult ORDER BY timestamp DESC LIMIT :n")
    List<QuizResult> recentResults(int n);

    @Query("SELECT COUNT(*) FROM QuizResult")
    int resultCount();

    // ---- Chat ----
    @Insert
    void insertChat(ChatMessage m);

    @Query("SELECT * FROM ChatMessage ORDER BY id ASC")
    List<ChatMessage> chat();

    @Query("DELETE FROM ChatMessage")
    void clearChat();
}
