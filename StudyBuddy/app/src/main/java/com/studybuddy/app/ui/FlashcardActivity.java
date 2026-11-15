package com.studybuddy.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.studybuddy.app.R;
import com.studybuddy.app.data.AppDatabase;
import com.studybuddy.app.data.Question;
import com.studybuddy.app.util.Async;
import com.studybuddy.app.util.Prefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Flip-card study mode. Missed cards come back until you know them. */
public class FlashcardActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT = "subject";

    private final List<Question> deck = new ArrayList<>();
    private final Set<Long> retried = new HashSet<>();
    private boolean flipped = false;
    private int xp = 0;
    private int known = 0;
    private int total = 0;

    private TextView tvCount;
    private TextView tvSide;
    private TextView tvCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);
        setTitle("🃏 Flashcards");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        tvCount = findViewById(R.id.tv_count);
        tvSide = findViewById(R.id.tv_side);
        tvCard = findViewById(R.id.tv_card);
        findViewById(R.id.card_flash).setOnClickListener(v -> flip());
        findViewById(R.id.btn_know).setOnClickListener(v -> next(true));
        findViewById(R.id.btn_again).setOnClickListener(v -> next(false));

        final long subjectId = getIntent().getLongExtra(EXTRA_SUBJECT, 0);
        final Context appContext = getApplicationContext();
        Async.<List<Question>>load(() -> AppDatabase.get(appContext).dao().randomQuestions(subjectId, 10), list -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (list == null || list.isEmpty()) {
                Toast.makeText(this, "No cards yet — add some notes in the Library!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            deck.addAll(list);
            total = list.size();
            show();
        });
    }

    private void show() {
        flipped = false;
        Question q = deck.get(0);
        tvCount.setText(deck.size() + " card" + (deck.size() == 1 ? "" : "s") + " left");
        tvSide.setText("QUESTION");
        tvCard.setText(q.question);
    }

    private void flip() {
        if (deck.isEmpty()) {
            return;
        }
        Question q = deck.get(0);
        flipped = !flipped;
        if (flipped) {
            tvSide.setText("ANSWER");
            tvCard.setText("✅ " + q.correctText() + "\n\n" + q.explanation);
        } else {
            tvSide.setText("QUESTION");
            tvCard.setText(q.question);
        }
    }

    private void next(boolean gotIt) {
        if (deck.isEmpty()) {
            return;
        }
        Question q = deck.remove(0);
        if (gotIt) {
            known++;
            xp += retried.contains(q.id) ? 1 : 3;
        } else {
            retried.add(q.id);
            deck.add(q);
        }
        if (deck.isEmpty()) {
            finishDeck();
        } else {
            show();
        }
    }

    private void finishDeck() {
        Prefs prefs = new Prefs(getApplicationContext());
        prefs.addXp(xp);
        prefs.markStudiedToday();
        new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 Deck complete!")
                .setMessage("You learned " + known + " of " + total + " cards.\n\n⭐ +" + xp + " XP")
                .setCancelable(false)
                .setPositiveButton("Done", (d, w) -> finish())
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
