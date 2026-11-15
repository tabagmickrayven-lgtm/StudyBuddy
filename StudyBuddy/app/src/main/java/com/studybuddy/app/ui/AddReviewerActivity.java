package com.studybuddy.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.studybuddy.app.R;
import com.studybuddy.app.ai.LocalAI;
import com.studybuddy.app.data.AppDatabase;
import com.studybuddy.app.data.Question;
import com.studybuddy.app.data.Reviewer;
import com.studybuddy.app.data.StudyDao;
import com.studybuddy.app.data.Subject;
import com.studybuddy.app.util.Async;

import java.util.ArrayList;
import java.util.List;

/** Lets the user paste notes; the on-device AI summarizes them and generates quiz questions. */
public class AddReviewerActivity extends AppCompatActivity {

    private static final String[] EMOJIS = {"📘", "🧪", "🌍", "💡", "🎨", "🧮", "⚽", "🎵", "💻", "🌱"};

    private Spinner spinner;
    private TextInputEditText etNewSubject;
    private TextInputEditText etTitle;
    private TextInputEditText etContent;
    private MaterialButton btnSave;
    private final List<Long> subjectIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reviewer);
        setTitle("Add notes");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        spinner = findViewById(R.id.sp_subject);
        etNewSubject = findViewById(R.id.et_new_subject);
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> save());

        final Context appContext = getApplicationContext();
        Async.<List<Subject>>load(() -> AppDatabase.get(appContext).dao().subjects(), subjects -> {
            if (isFinishing() || isDestroyed() || subjects == null) {
                return;
            }
            List<String> labels = new ArrayList<>();
            for (Subject s : subjects) {
                labels.add(s.emoji + " " + s.name);
                subjectIds.add(s.id);
            }
            spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        });
    }

    private void save() {
        final String title = text(etTitle);
        final String content = text(etContent);
        final String newSubject = text(etNewSubject);
        if (title.isEmpty()) {
            etTitle.setError("Give your notes a title");
            return;
        }
        if (content.length() < 40) {
            etContent.setError("Write a few full sentences so the AI has something to work with");
            return;
        }
        if (newSubject.isEmpty() && subjectIds.isEmpty()) {
            etNewSubject.setError("Create a subject first");
            return;
        }
        final int pos = spinner.getSelectedItemPosition();
        final List<Long> ids = new ArrayList<>(subjectIds);
        final Context appContext = getApplicationContext();
        btnSave.setEnabled(false);
        btnSave.setText("🤖 AI is working…");

        Async.<Integer>load(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            long subjectId;
            if (!newSubject.isEmpty()) {
                String emoji = EMOJIS[(int) (System.currentTimeMillis() % EMOJIS.length)];
                subjectId = dao.insertSubject(new Subject(newSubject, emoji));
            } else {
                subjectId = ids.get(Math.max(0, pos));
            }
            Reviewer r = new Reviewer(subjectId, title, content);
            r.summary = LocalAI.summarize(content, 3);
            r.id = dao.insertReviewer(r);
            List<Question> qs = LocalAI.generateQuestions(r, 10, LocalAI.termPool(dao.reviewers()));
            dao.insertQuestions(qs);
            return qs.size();
        }, n -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            int count = n == null ? 0 : n;
            Toast.makeText(this, count > 0
                    ? "✨ Saved! The AI created " + count + " questions."
                    : "Saved! (Notes were short, so no questions were generated.)", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
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
