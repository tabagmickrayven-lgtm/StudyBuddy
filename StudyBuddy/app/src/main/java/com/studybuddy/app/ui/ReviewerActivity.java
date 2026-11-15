package com.studybuddy.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.studybuddy.app.R;
import com.studybuddy.app.ai.LocalAI;
import com.studybuddy.app.data.AppDatabase;
import com.studybuddy.app.data.Question;
import com.studybuddy.app.data.Reviewer;
import com.studybuddy.app.data.StudyDao;
import com.studybuddy.app.data.Subject;
import com.studybuddy.app.util.Async;

import java.util.List;

public class ReviewerActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";

    public static Intent intent(Context c, long reviewerId) {
        Intent i = new Intent(c, ReviewerActivity.class);
        i.putExtra(EXTRA_ID, reviewerId);
        return i;
    }

    private static final class Loaded {
        Reviewer reviewer;
        Subject subject;
    }

    private long reviewerId;
    private TextView tvTitle;
    private TextView tvSubject;
    private TextView tvSummary;
    private TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviewer);
        setTitle("Reviewer");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        reviewerId = getIntent().getLongExtra(EXTRA_ID, 0);
        tvTitle = findViewById(R.id.tv_title);
        tvSubject = findViewById(R.id.tv_subject);
        tvSummary = findViewById(R.id.tv_summary);
        tvContent = findViewById(R.id.tv_content);

        findViewById(R.id.btn_quiz).setOnClickListener(v ->
                startActivity(QuizActivity.intent(this, "classic", 0, reviewerId, 8)));
        findViewById(R.id.btn_regen).setOnClickListener(v -> regenerate());
        findViewById(R.id.btn_delete).setOnClickListener(v -> confirmDelete());

        final Context appContext = getApplicationContext();
        Async.<Loaded>load(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            Loaded l = new Loaded();
            l.reviewer = dao.reviewer(reviewerId);
            if (l.reviewer != null) {
                l.subject = dao.subject(l.reviewer.subjectId);
                if (l.reviewer.summary == null || l.reviewer.summary.isEmpty()) {
                    l.reviewer.summary = LocalAI.summarize(l.reviewer.content, 3);
                    dao.setSummary(l.reviewer.id, l.reviewer.summary);
                }
            }
            return l;
        }, l -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (l == null || l.reviewer == null) {
                finish();
                return;
            }
            tvTitle.setText(l.reviewer.title);
            tvSubject.setText(l.subject == null ? "" : l.subject.emoji + " " + l.subject.name);
            tvSummary.setText(l.reviewer.summary);
            tvContent.setText(l.reviewer.content);
        });
    }

    private void regenerate() {
        final Context appContext = getApplicationContext();
        Toast.makeText(this, "🤖 Reading your notes…", Toast.LENGTH_SHORT).show();
        Async.<Integer>load(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            Reviewer r = dao.reviewer(reviewerId);
            if (r == null) {
                return 0;
            }
            dao.deleteAiQuestionsFor(r.id);
            List<Question> qs = LocalAI.generateQuestions(r, 8, LocalAI.termPool(dao.reviewers()));
            dao.insertQuestions(qs);
            return qs.size();
        }, n -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            int count = n == null ? 0 : n;
            Toast.makeText(this, count == 0
                    ? "These notes are too short to make questions. Try adding more sentences."
                    : "✨ Created " + count + " fresh questions!", Toast.LENGTH_LONG).show();
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this reviewer?")
                .setMessage("Its questions will be deleted too.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    final Context appContext = getApplicationContext();
                    Async.<Boolean>load(() -> {
                        StudyDao dao = AppDatabase.get(appContext).dao();
                        dao.deleteQuestionsFor(reviewerId);
                        dao.deleteReviewerById(reviewerId);
                        return true;
                    }, ok -> finish());
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
