package com.studybuddy.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.studybuddy.app.R;
import com.studybuddy.app.data.AppDatabase;
import com.studybuddy.app.data.QuizResult;
import com.studybuddy.app.data.StudyDao;
import com.studybuddy.app.data.Subject;
import com.studybuddy.app.util.Async;
import com.studybuddy.app.util.Prefs;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final class Stats {
        String stats;
        String recent;
    }

    private TextView tvGreeting;
    private TextView tvLevel;
    private TextView tvStreak;
    private TextView tvStats;
    private TextView tvRecent;
    private ProgressBar progressXp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        tvGreeting = v.findViewById(R.id.tv_greeting);
        tvLevel = v.findViewById(R.id.tv_level);
        tvStreak = v.findViewById(R.id.tv_streak);
        tvStats = v.findViewById(R.id.tv_stats);
        tvRecent = v.findViewById(R.id.tv_recent);
        progressXp = v.findViewById(R.id.progress_xp);

        v.findViewById(R.id.btn_daily).setOnClickListener(x ->
                startActivity(QuizActivity.intent(requireContext(), "classic", 0, 0, 5)));
        v.findViewById(R.id.btn_teacher).setOnClickListener(x -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_nav);
            nav.setSelectedItemId(R.id.nav_teacher);
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        final Prefs prefs = new Prefs(requireContext());
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String hello = hour < 12 ? "Good morning ☀️" : hour < 18 ? "Good afternoon 🌤️" : "Good evening 🌙";
        tvGreeting.setText(hello);
        tvLevel.setText("Level " + prefs.level() + "  •  " + prefs.xp() + " XP");
        progressXp.setProgress(prefs.xpIntoLevel());
        int streak = prefs.streak();
        if (streak > 0) {
            tvStreak.setText("🔥 " + streak + "-day streak" + (prefs.studiedToday() ? " — done for today!" : " — study today to keep it!"));
        } else {
            tvStreak.setText("🔥 Start a streak — finish a quiz today!");
        }

        final android.content.Context appContext = requireContext().getApplicationContext();
        Async.<Stats>load(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            Map<Long, Subject> subjects = new HashMap<>();
            for (Subject s : dao.subjects()) {
                subjects.put(s.id, s);
            }
            List<QuizResult> results = dao.recentResults(30);
            int score = 0;
            int total = 0;
            for (QuizResult r : results) {
                score += r.score;
                total += r.total;
            }
            Stats st = new Stats();
            int pct = total == 0 ? 0 : Math.round(100f * score / total);
            st.stats = "📝 " + dao.reviewers().size() + " reviewers\n"
                    + "❓ " + dao.questionCount() + " questions in your database\n"
                    + "🏆 " + dao.resultCount() + " quizzes taken\n"
                    + "🎯 " + (total == 0 ? "No accuracy yet" : pct + "% accuracy (last 30 quizzes)");
            StringBuilder sb = new StringBuilder();
            DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
            int shown = 0;
            for (QuizResult r : results) {
                if (shown++ >= 5) {
                    break;
                }
                Subject s = subjects.get(r.subjectId);
                String name = s == null ? "🌈 Mixed" : s.emoji + " " + s.name;
                sb.append(name).append(" — ").append(r.score).append("/").append(r.total)
                        .append("  (").append(r.mode).append(", ").append(df.format(new Date(r.timestamp))).append(")\n");
            }
            st.recent = sb.length() == 0 ? "Nothing yet. Tap Daily Challenge to begin!" : sb.toString().trim();
            return st;
        }, st -> {
            if (!isAdded() || st == null) {
                return;
            }
            tvStats.setText(st.stats);
            tvRecent.setText(st.recent);
        });
    }
}
