package com.studybuddy.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.studybuddy.app.R;
import com.studybuddy.app.ai.LocalAI;
import com.studybuddy.app.data.AppDatabase;
import com.studybuddy.app.data.ChatMessage;
import com.studybuddy.app.data.QuizResult;
import com.studybuddy.app.data.Reviewer;
import com.studybuddy.app.data.StudyDao;
import com.studybuddy.app.data.Subject;
import com.studybuddy.app.util.Async;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Chat with the built-in AI Teacher. Conversation history is stored in the local database. */
public class TeacherFragment extends Fragment {

    private static final String WELCOME =
            "Hi! I'm your AI Teacher 🎓\n\nI read your reviewers and can explain topics, summarize notes, "
                    + "and quiz you — all on your phone.\n\nTry: \"What is photosynthesis?\" or \"Quiz me\".";

    private final List<ChatMessage> messages = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ChatAdapter adapter;
    private RecyclerView rv;
    private EditText etMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher, container, false);
        rv = v.findViewById(R.id.rv_chat);
        etMessage = v.findViewById(R.id.et_message);
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        adapter = new ChatAdapter();
        rv.setAdapter(adapter);

        v.findViewById(R.id.btn_send).setOnClickListener(x -> send(etMessage.getText().toString()));
        etMessage.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send(etMessage.getText().toString());
                return true;
            }
            return false;
        });
        v.findViewById(R.id.chip_quiz).setOnClickListener(x -> send("Quiz me"));
        v.findViewById(R.id.chip_study).setOnClickListener(x -> send("What should I study?"));
        v.findViewById(R.id.chip_tips).setOnClickListener(x -> send("Give me study tips"));
        v.findViewById(R.id.chip_explain).setOnClickListener(x -> send("Explain photosynthesis"));
        v.findViewById(R.id.btn_clear).setOnClickListener(x -> clearChat());

        loadHistory();
        return v;
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void loadHistory() {
        final Context appContext = requireContext().getApplicationContext();
        Async.<List<ChatMessage>>load(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            List<ChatMessage> history = dao.chat();
            if (history.isEmpty()) {
                ChatMessage welcome = new ChatMessage(false, WELCOME);
                dao.insertChat(welcome);
                history = new ArrayList<>();
                history.add(welcome);
            }
            return history;
        }, history -> {
            if (!isAdded() || history == null) {
                return;
            }
            messages.clear();
            messages.addAll(history);
            adapter.notifyDataSetChanged();
            scrollToEnd();
        });
    }

    private void clearChat() {
        final Context appContext = requireContext().getApplicationContext();
        messages.clear();
        final ChatMessage welcome = new ChatMessage(false, WELCOME);
        messages.add(welcome);
        adapter.notifyDataSetChanged();
        Async.io(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            dao.clearChat();
            dao.insertChat(welcome);
        });
    }

    private void scrollToEnd() {
        if (!messages.isEmpty()) {
            rv.scrollToPosition(messages.size() - 1);
        }
    }

    private void send(String raw) {
        final String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return;
        }
        etMessage.setText("");
        final Context appContext = requireContext().getApplicationContext();

        final ChatMessage userMsg = new ChatMessage(true, text);
        messages.add(userMsg);
        final ChatMessage reply = new ChatMessage(false, "💭 …");
        messages.add(reply);
        adapter.notifyItemRangeInserted(messages.size() - 2, 2);
        scrollToEnd();

        Async.<LocalAI.Answer>load(() -> {
            StudyDao dao = AppDatabase.get(appContext).dao();
            dao.insertChat(userMsg);
            List<Reviewer> revs = dao.reviewers();
            List<QuizResult> results = dao.recentResults(30);
            Map<Long, String> names = new HashMap<>();
            for (Subject s : dao.subjects()) {
                names.put(s.id, s.name);
            }
            return LocalAI.reply(text, revs, results, names);
        }, answer -> {
            if (!isAdded() || answer == null) {
                return;
            }
            // A short "typing" pause makes the teacher feel more natural.
            handler.postDelayed(() -> {
                if (!isAdded()) {
                    return;
                }
                reply.text = answer.text;
                int pos = messages.indexOf(reply);
                if (pos >= 0) {
                    adapter.notifyItemChanged(pos);
                }
                scrollToEnd();
                Async.io(() -> AppDatabase.get(appContext).dao().insertChat(reply));
                if (answer.action == LocalAI.Action.QUIZ) {
                    handler.postDelayed(() -> {
                        if (isAdded()) {
                            startActivity(QuizActivity.intent(requireContext(), "classic", answer.subjectId, 0, 5));
                        }
                    }, 900);
                }
            }, 450);
        });
    }

    private final class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

        final class VH extends RecyclerView.ViewHolder {
            final TextView tv;

            VH(View v) {
                super(v);
                tv = v.findViewById(R.id.tv_msg);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ChatMessage m = messages.get(position);
            h.tv.setText(m.text);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) h.tv.getLayoutParams();
            lp.gravity = m.fromUser ? Gravity.END : Gravity.START;
            h.tv.setLayoutParams(lp);
            h.tv.setBackgroundResource(m.fromUser ? R.drawable.bubble_user : R.drawable.bubble_ai);
            h.tv.setTextColor(ContextCompat.getColor(h.tv.getContext(),
                    m.fromUser ? R.color.white : R.color.text));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }
}
