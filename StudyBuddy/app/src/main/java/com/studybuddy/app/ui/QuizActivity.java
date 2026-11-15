package com.studybuddy.app.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.studybuddy.app.R;
import com.studybuddy.app.data.AppDatabase;
import com.studybuddy.app.data.Question;
import com.studybuddy.app.data.QuizResult;
import com.studybuddy.app.data.StudyDao;
import com.studybuddy.app.util.Async;
import com.studybuddy.app.util.Prefs;

import java.util.List;

/** Classic and Speed Round quizzes with combos, XP and animations. */
public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_REVIEWER = "reviewer";
    public static final String EXTRA_COUNT = "count";

    public static Intent intent(Context c, String mode, long subjectId, long reviewerId, int count) {
        Intent i = new Intent(c, QuizActivity.class);
        i.putExtra(EXTRA_MODE, mode);
        i.putExtra(EXTRA_SUBJECT, subjectId);
        i.putExtra(EXTRA_REVIEWER, reviewerId);
        i.putExtra(EXTRA_COUNT, count);
        return i;
    }

    private List<Question> questions;
    private int index = 0;
    private int score = 0;
    private int combo = 0;
    private int bestCombo = 0;
    private int xp = 0;
    private int secondsLeft = 15;
    private boolean answered = false;
    private boolean speed;
    private long subjectId;
    private String mode;
    private CountDownTimer timer;

    private TextView tvProgress;
    private TextView tvCombo;
    private TextView tvTimer;
    private TextView tvQuestion;
    private TextView tvFeedback;
    private ProgressBar bar;
    private View card;
    private MaterialButton btnNext;
    private final MaterialButton[] buttons = new MaterialButton[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) {
            mode = "classic";
        }
        speed = "speed".equals(mode);
        subjectId = getIntent().getLongExtra(EXTRA_SUBJECT, 0);
        final long reviewerId = getIntent().getLongExtra(EXTRA_REVIEWER, 0);
        final int count = getIntent().getIntExtra(EXTRA_COUNT, 10);

        setTitle(speed ? "⚡ Speed Round" : "🧠 Quiz");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvProgress = findViewById(R.id.tv_progress);
        tvCombo = findViewById(R.id.tv_combo);
        tvTimer = findViewById(R.id.tv_timer);
        tvQuestion = findViewById(R.id.tv_question);
        tvFeedback = findViewById(R.id.tv_feedback);
        bar = findViewById(R.id.progress_bar);
        card = findViewById(R.id.card_question);
        btnNext = findViewById(R.id.btn_next);
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3};
        for (int i = 0; i < 4; i++) {
            final int choice = i;
            buttons[i] = findViewById(ids[i]);
            buttons[i].setOnClickListener(v -> answer(choice));
        }
        btnNext.setOnClickListener(v -> {
            index++;
            if (index >= questions.size()) {
                finishQuiz();
            } else {
                showQuestion();
            }
        });

        final Context appContext = getApplicationContext();
        Async.<List<Question>>load(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            return reviewerId > 0 ? dao.randomForReviewer(reviewerId, count) : dao.randomQuestions(subjectId, count);
        }, list -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (list == null || list.isEmpty()) {
                Toast.makeText(this, "No questions yet — add some notes in the Library!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            questions = list;
            bar.setMax(questions.size());
            showQuestion();
        });
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }

    private void showQuestion() {
        answered = false;
        Question q = questions.get(index);
        tvProgress.setText("Question " + (index + 1) + " / " + questions.size());
        bar.setProgress(index);
        tvQuestion.setText(q.question);
        tvFeedback.setText("");
        tvCombo.setText(combo >= 2 ? "🔥 x" + combo : "");
        String[] options = q.options();
        for (int i = 0; i < 4; i++) {
            buttons[i].setText(options[i]);
            buttons[i].setEnabled(true);
            buttons[i].setBackgroundTintList(ColorStateList.valueOf(color(R.color.primary)));
        }
        btnNext.setVisibility(View.INVISIBLE);
        if (speed) {
            startTimer();
        } else {
            tvTimer.setText("");
        }
    }

    private void startTimer() {
        cancelTimer();
        secondsLeft = 15;
        tvTimer.setText("⏱ 15");
        timer = new CountDownTimer(15000, 250) {
            @Override
            public void onTick(long ms) {
                secondsLeft = (int) Math.ceil(ms / 1000.0);
                tvTimer.setText("⏱ " + secondsLeft);
            }

            @Override
            public void onFinish() {
                if (!answered) {
                    answer(-1);
                }
            }
        }.start();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void answer(int choice) {
        if (answered) {
            return;
        }
        answered = true;
        cancelTimer();
        Question q = questions.get(index);
        boolean ok = choice == q.correct;
        for (MaterialButton b : buttons) {
            b.setEnabled(false);
        }
        buttons[q.correct].setBackgroundTintList(ColorStateList.valueOf(color(R.color.good)));
        if (ok) {
            score++;
            combo++;
            bestCombo = Math.max(bestCombo, combo);
            int gained = 10;
            if (speed) {
                gained += Math.min(5, secondsLeft / 3);
            }
            if (combo >= 3) {
                gained += 2;
            }
            xp += gained;
            tvFeedback.setText("✅ Correct! +" + gained + " XP\n\n" + q.explanation);
            card.animate().scaleX(1.04f).scaleY(1.04f).setDuration(120)
                    .withEndAction(() -> card.animate().scaleX(1f).scaleY(1f).setDuration(120));
        } else {
            combo = 0;
            if (choice >= 0) {
                buttons[choice].setBackgroundTintList(ColorStateList.valueOf(color(R.color.bad)));
            }
            String lead = choice < 0 ? "⏰ Time's up!" : "❌ Not quite.";
            tvFeedback.setText(lead + " The answer is: " + q.correctText() + "\n\n" + q.explanation);
            ObjectAnimator.ofFloat(card, "translationX", 0f, -20f, 20f, -12f, 12f, 0f).setDuration(320).start();
        }
        tvCombo.setText(combo >= 2 ? "🔥 x" + combo : "");
        btnNext.setText(index == questions.size() - 1 ? "Finish 🏁" : "Next ➜");
        btnNext.setVisibility(View.VISIBLE);
    }

    private void finishQuiz() {
        bar.setProgress(questions.size());
        final int total = questions.size();
        final QuizResult result = new QuizResult(subjectId, mode, score, total, xp);
        final Context appContext = getApplicationContext();
        Prefs prefs = new Prefs(appContext);
        int levelBefore = prefs.level();
        prefs.addXp(xp);
        prefs.markStudiedToday();
        boolean levelUp = prefs.level() > levelBefore;
        Async.io(() -> AppDatabase.get(appContext).dao().insertResult(result));

        float pct = total == 0 ? 0 : (float) score / total;
        String verdict = pct >= 0.9f ? "🏆 Outstanding!" : pct >= 0.7f ? "🎉 Great job!"
                : pct >= 0.5f ? "👍 Nice effort!" : "💪 Keep practicing!";
        String message = "You scored " + score + " / " + total + "\n\n"
                + "⭐ +" + xp + " XP\n"
                + "🔥 Best combo: x" + bestCombo
                + (levelUp ? "\n\n🆙 LEVEL UP! You're now level " + prefs.level() + "!" : "");
        new MaterialAlertDialogBuilder(this)
                .setTitle(verdict)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Done", (d, w) -> finish())
                .setNegativeButton("Play again", (d, w) -> {
                    startActivity(getIntent());
                    finish();
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

    @Override
    protected void onDestroy() {
        cancelTimer();
        super.onDestroy();
    }
}
