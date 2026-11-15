package com.studybuddy.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.studybuddy.app.R;
import com.studybuddy.app.data.AppDatabase;
import com.studybuddy.app.data.Reviewer;
import com.studybuddy.app.data.StudyDao;
import com.studybuddy.app.data.Subject;
import com.studybuddy.app.util.Async;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryFragment extends Fragment {

    private static final class Row {
        long id;
        String title;
        String sub;
        String preview;
        String emoji;
    }

    private static final class Data {
        List<Subject> subjects = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
    }

    private final List<Row> rows = new ArrayList<>();
    private RowAdapter adapter;
    private ChipGroup chips;
    private TextView tvEmpty;
    private long filterSubject = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library, container, false);
        chips = v.findViewById(R.id.chip_group);
        tvEmpty = v.findViewById(R.id.tv_empty);
        RecyclerView rv = v.findViewById(R.id.rv_reviewers);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RowAdapter();
        rv.setAdapter(adapter);
        v.findViewById(R.id.btn_add).setOnClickListener(x ->
                startActivity(new Intent(requireContext(), AddReviewerActivity.class)));
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        final Context appContext = requireContext().getApplicationContext();
        final long filter = filterSubject;
        Async.<Data>load(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            Data d = new Data();
            d.subjects = dao.subjects();
            Map<Long, Subject> map = new HashMap<>();
            for (Subject s : d.subjects) {
                map.put(s.id, s);
            }
            for (Reviewer r : dao.reviewers()) {
                if (filter != 0 && r.subjectId != filter) {
                    continue;
                }
                Subject s = map.get(r.subjectId);
                Row row = new Row();
                row.id = r.id;
                row.title = r.title;
                row.emoji = s == null ? "📘" : s.emoji;
                row.sub = (s == null ? "" : s.name + " • ") + dao.questionCountFor(r.id) + " questions";
                row.preview = r.content;
                d.rows.add(row);
            }
            return d;
        }, d -> {
            if (!isAdded() || d == null) {
                return;
            }
            buildChips(d.subjects);
            rows.clear();
            rows.addAll(d.rows);
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void buildChips(List<Subject> subjects) {
        chips.removeAllViews();
        addChip("All", 0);
        for (Subject s : subjects) {
            addChip(s.emoji + " " + s.name, s.id);
        }
    }

    private void addChip(String label, long subjectId) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setTag(subjectId);
        chip.setChecked(subjectId == filterSubject);
        chip.setOnClickListener(v -> {
            filterSubject = (Long) v.getTag();
            load();
        });
        chips.addView(chip);
    }

    private final class RowAdapter extends RecyclerView.Adapter<RowAdapter.VH> {

        final class VH extends RecyclerView.ViewHolder {
            final TextView emoji;
            final TextView title;
            final TextView sub;
            final TextView preview;

            VH(View v) {
                super(v);
                emoji = v.findViewById(R.id.tv_emoji);
                title = v.findViewById(R.id.tv_title);
                sub = v.findViewById(R.id.tv_sub);
                preview = v.findViewById(R.id.tv_preview);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reviewer, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final Row r = rows.get(position);
            h.emoji.setText(r.emoji);
            h.title.setText(r.title);
            h.sub.setText(r.sub);
            h.preview.setText(r.preview);
            h.itemView.setOnClickListener(v -> startActivity(ReviewerActivity.intent(v.getContext(), r.id)));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }
}
