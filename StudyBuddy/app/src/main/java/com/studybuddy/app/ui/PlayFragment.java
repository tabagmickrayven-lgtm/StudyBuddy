package com.studybuddy.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.studybuddy.app.R;
import com.studybuddy.app.data.AppDatabase;
import com.studybuddy.app.data.Subject;
import com.studybuddy.app.util.Async;

import java.util.ArrayList;
import java.util.List;

public class PlayFragment extends Fragment {

    private Spinner spinner;
    private TextView tvHint;
    private final List<Long> subjectIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_play, container, false);
        spinner = v.findViewById(R.id.sp_subject);
        tvHint = v.findViewById(R.id.tv_hint);

        v.findViewById(R.id.btn_classic).setOnClickListener(x ->
                startActivity(QuizActivity.intent(requireContext(), "classic", selectedSubject(), 0, 10)));
        v.findViewById(R.id.btn_speed).setOnClickListener(x ->
                startActivity(QuizActivity.intent(requireContext(), "speed", selectedSubject(), 0, 10)));
        v.findViewById(R.id.btn_flash).setOnClickListener(x -> {
            Intent i = new Intent(requireContext(), FlashcardActivity.class);
            i.putExtra(FlashcardActivity.EXTRA_SUBJECT, selectedSubject());
            startActivity(i);
        });
        return v;
    }

    private long selectedSubject() {
        int pos = spinner.getSelectedItemPosition();
        if (pos < 0 || pos >= subjectIds.size()) {
            return 0;
        }
        return subjectIds.get(pos);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Context appContext = requireContext().getApplicationContext();
        Async.<List<Subject>>load(() -> AppDatabase.get(appContext).dao().subjects(), subjects -> {
            if (!isAdded() || subjects == null) {
                return;
            }
            long previous = selectedSubject();
            List<String> labels = new ArrayList<>();
            subjectIds.clear();
            labels.add("🌈 All subjects");
            subjectIds.add(0L);
            for (Subject s : subjects) {
                labels.add(s.emoji + " " + s.name);
                subjectIds.add(s.id);
            }
            ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, labels);
            spinner.setAdapter(a);
            int idx = subjectIds.indexOf(previous);
            spinner.setSelection(idx < 0 ? 0 : idx);
            tvHint.setText("Every quiz is built from the questions saved in your on-device database — including the ones the AI generated from your notes.");
        });
    }
}
