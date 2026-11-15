package com.studybuddy.app.ai;

import com.studybuddy.app.data.Question;
import com.studybuddy.app.data.QuizResult;
import com.studybuddy.app.data.Reviewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * StudyBuddy's fully on-device "AI". Nothing leaves the phone and no internet is needed.
 *
 * It combines classic NLP techniques:
 *  - TF-IDF sentence retrieval  -> the AI Teacher answers from YOUR reviewers
 *  - Extractive summarization   -> short summaries of any reviewer
 *  - Cloze question generation  -> automatic multiple-choice questions from any notes
 *  - Performance analysis       -> tells you what to study next
 */
public final class LocalAI {

    private LocalAI() {
    }

    // ------------------------------------------------------------------
    // Public types
    // ------------------------------------------------------------------

    public enum Action {NONE, QUIZ}

    public static class Answer {
        public final String text;
        public final Action action;
        public final long subjectId;

        Answer(String text) {
            this(text, Action.NONE, 0);
        }

        Answer(String text, Action action, long subjectId) {
            this.text = text;
            this.action = action;
            this.subjectId = subjectId;
        }
    }

    // ------------------------------------------------------------------
    // Text utilities
    // ------------------------------------------------------------------

    private static final Random RND = new Random();

    private static final Set<String> STOP = new HashSet<>(Arrays.asList((
            "a an the is are was were be been being of to in on at for and or but if then than that this these "
                    + "those it its as by with from into about what which who whom whose how why when where do does "
                    + "did can could should would will shall may might must i me my you your we our they their he "
                    + "she his her them us not no yes so too very just also have has had tell explain define "
                    + "describe give show please there here some any each every more most such only own same "
                    + "other another over under between during before after above below because while").split(" ")));

    static String stem(String w) {
        if (w.length() > 4 && w.endsWith("ies")) {
            return w.substring(0, w.length() - 3) + "y";
        }
        if (w.length() > 5 && (w.endsWith("sses") || w.endsWith("ches") || w.endsWith("shes") || w.endsWith("xes"))) {
            return w.substring(0, w.length() - 2);
        }
        if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss") && !w.endsWith("us")) {
            return w.substring(0, w.length() - 1);
        }
        return w;
    }

    /** Lower-cased, stop-word-free, stemmed tokens. */
    public static List<String> tokens(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) {
            return out;
        }
        for (String w : s.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (w.isEmpty() || STOP.contains(w)) {
                continue;
            }
            boolean number = w.matches("\\d+");
            if (w.length() < 3 && !number) {
                continue;
            }
            out.add(stem(w));
        }
        return out;
    }

    public static List<String> splitSentences(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String part : text.replace("\r", " ").split("(?<=[.!?])\\s+|\\n+")) {
            String t = part.trim();
            if (t.length() >= 15) {
                out.add(t);
            }
        }
        return out;
    }

    private static Map<String, Integer> docFreq(List<String> sentences) {
        Map<String, Integer> df = new HashMap<>();
        for (String s : sentences) {
            for (String t : new HashSet<>(tokens(s))) {
                df.merge(t, 1, Integer::sum);
            }
        }
        return df;
    }

    private static String pick(String[] options) {
        return options[RND.nextInt(options.length)];
    }

    // ------------------------------------------------------------------
    // Retrieval (TF-IDF over sentences of all reviewers)
    // ------------------------------------------------------------------

    private static final class Sent {
        final String text;
        final String title;
        final Set<String> set;
        final Set<String> titleSet;

        Sent(String text, String title) {
            this.text = text;
            this.title = title;
            this.set = new HashSet<>(tokens(text));
            this.titleSet = new HashSet<>(tokens(title));
        }
    }

    private static List<Sent> top(String query, List<Reviewer> revs, int k) {
        List<Sent> idx = new ArrayList<>();
        for (Reviewer r : revs) {
            for (String s : splitSentences(r.content)) {
                idx.add(new Sent(s, r.title));
            }
        }
        if (idx.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Integer> df = new HashMap<>();
        for (Sent s : idx) {
            for (String t : s.set) {
                df.merge(t, 1, Integer::sum);
            }
        }
        List<String> qt = new ArrayList<>(new LinkedHashSet<>(tokens(query)));
        final Map<Sent, Double> score = new HashMap<>();
        for (Sent s : idx) {
            double sc = 0;
            for (String t : qt) {
                Integer d = df.get(t);
                double idf = Math.log(1.0 + (double) idx.size() / (1 + (d == null ? 0 : d)));
                if (s.set.contains(t)) {
                    sc += idf;
                }
                if (s.titleSet.contains(t)) {
                    sc += 0.5 * idf;
                }
            }
            if (sc > 0) {
                score.put(s, sc);
            }
        }
        List<Sent> out = new ArrayList<>(score.keySet());
        Collections.sort(out, (x, y) -> Double.compare(score.get(y), score.get(x)));
        return new ArrayList<>(out.subList(0, Math.min(k, out.size())));
    }

    // ------------------------------------------------------------------
    // Summarization
    // ------------------------------------------------------------------

    public static String summarize(String text, int n) {
        List<String> ss = splitSentences(text);
        if (ss.isEmpty()) {
            return "";
        }
        List<Integer> chosen = new ArrayList<>();
        if (ss.size() <= n) {
            for (int i = 0; i < ss.size(); i++) {
                chosen.add(i);
            }
        } else {
            final Map<String, Integer> freq = new HashMap<>();
            for (String s : ss) {
                for (String t : tokens(s)) {
                    freq.merge(t, 1, Integer::sum);
                }
            }
            final double[] scores = new double[ss.size()];
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < ss.size(); i++) {
                List<String> toks = tokens(ss.get(i));
                double sc = 0;
                for (String t : toks) {
                    sc += freq.get(t);
                }
                scores[i] = sc / Math.max(4, toks.size()) + (i == 0 ? 1.5 : 0);
                order.add(i);
            }
            Collections.sort(order, (x, y) -> Double.compare(scores[y], scores[x]));
            chosen.addAll(order.subList(0, n));
            Collections.sort(chosen);
        }
        StringBuilder sb = new StringBuilder();
        for (int i : chosen) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("• ").append(ss.get(i));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Question generation (cloze / fill-in-the-blank with smart distractors)
    // ------------------------------------------------------------------

    private static String clean(String w) {
        return w.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}%]+$", "");
    }

    private static String bestTerm(String sentence, Map<String, Integer> df) {
        String[] words = sentence.split("\\s+");
        String best = null;
        double bestScore = 0;
        for (int i = 0; i < words.length; i++) {
            String w = clean(words[i]);
            if (w.length() < 2) {
                continue;
            }
            String lw = w.toLowerCase(Locale.ROOT);
            if (STOP.contains(lw)) {
                continue;
            }
            boolean num = w.matches("\\d+([.,]\\d+)?%?");
            boolean cap = i > 0 && Character.isUpperCase(w.charAt(0));
            if (!num && w.length() < 5) {
                continue;
            }
            if (i == 0 && !num) {
                continue;
            }
            double sc = 0;
            if (num) {
                sc += 3;
            }
            if (cap) {
                sc += 3;
            }
            if (w.length() >= 8) {
                sc += 2;
            } else if (w.length() >= 6) {
                sc += 1;
            }
            Integer d = df.get(stem(lw));
            if (d != null && d <= 1) {
                sc += 2;
            }
            if (sc > bestScore) {
                bestScore = sc;
                best = w;
            }
        }
        return bestScore >= 3 ? best : null;
    }

    /** Pool of key terms from all reviewers, used as wrong-answer choices. */
    public static List<String> termPool(List<Reviewer> revs) {
        Set<String> pool = new LinkedHashSet<>();
        for (Reviewer r : revs) {
            List<String> ss = splitSentences(r.content);
            Map<String, Integer> df = docFreq(ss);
            for (String s : ss) {
                String t = bestTerm(s, df);
                if (t != null && !t.matches("\\d.*")) {
                    pool.add(t);
                }
            }
        }
        return new ArrayList<>(pool);
    }

    private static List<String> distractors(String term, String sentence, List<String> pool) {
        List<String> out = new ArrayList<>();
        String low = sentence.toLowerCase(Locale.ROOT);
        if (term.matches("\\d+") && term.length() <= 9) {
            long n = Long.parseLong(term);
            long[] deltas = n >= 1000 ? new long[]{1, 2, 3, 5, 10, 20, 100}
                    : n >= 100 ? new long[]{10, 20, 30, 45, 90}
                    : new long[]{1, 2, 3, 5, 10};
            List<Long> cands = new ArrayList<>();
            for (long d : deltas) {
                if (n - d > 0) {
                    cands.add(n - d);
                }
                cands.add(n + d);
            }
            Collections.shuffle(cands, RND);
            for (Long c : cands) {
                String cs = String.valueOf(c);
                if (!cs.equals(term) && !out.contains(cs)) {
                    out.add(cs);
                }
                if (out.size() >= 3) {
                    break;
                }
            }
            return out;
        }
        boolean cap = Character.isUpperCase(term.charAt(0));
        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RND);
        for (String p : shuffled) {
            if (p.equalsIgnoreCase(term) || out.contains(p)) {
                continue;
            }
            if (low.contains(p.toLowerCase(Locale.ROOT)) || p.matches("\\d+")) {
                continue;
            }
            if (Character.isUpperCase(p.charAt(0)) != cap) {
                continue;
            }
            out.add(p);
            if (out.size() >= 3) {
                break;
            }
        }
        return out;
    }

    private static String blank(String sentence, String term) {
        Pattern p = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(term) + "(?![\\p{L}\\p{N}])");
        return p.matcher(sentence).replaceAll("_____");
    }

    /** Turns a reviewer's notes into multiple-choice questions. */
    public static List<Question> generateQuestions(Reviewer r, int max, List<String> pool) {
        List<String> sents = splitSentences(r.content);
        Map<String, Integer> df = docFreq(sents);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < sents.size(); i++) {
            order.add(i);
        }
        Collections.shuffle(order, RND);
        List<Question> out = new ArrayList<>();
        for (int i : order) {
            if (out.size() >= max) {
                break;
            }
            String s = sents.get(i);
            if (s.split("\\s+").length < 6) {
                continue;
            }
            String term = bestTerm(s, df);
            if (term == null) {
                continue;
            }
            if (term.matches("\\d.*") && !term.matches("\\d+")) {
                continue;
            }
            List<String> wrong = distractors(term, s, pool);
            if (wrong.size() < 3) {
                continue;
            }
            List<String> all = new ArrayList<>();
            all.add(term);
            all.addAll(wrong.subList(0, 3));
            Collections.shuffle(all, RND);

            Question q = new Question();
            q.subjectId = r.subjectId;
            q.reviewerId = r.id;
            q.question = "Fill in the blank:\n\n" + blank(s, term);
            q.a = all.get(0);
            q.b = all.get(1);
            q.c = all.get(2);
            q.d = all.get(3);
            q.correct = all.indexOf(term);
            q.explanation = s;
            q.source = 1;
            out.add(q);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // AI Teacher
    // ------------------------------------------------------------------

    private static final String[] GREETINGS = {
            "Hi there! 👋 I'm your AI Teacher. Ask me about anything in your reviewers, or say \"quiz me\"!",
            "Hello! 🎓 Ready to learn something? Ask a question, or try \"summarize Solar System\".",
            "Hey! 😄 What are we studying today? I can explain topics, summarize notes, or quiz you."
    };

    private static final String CAPABILITIES =
            "Here's what I can do — all on your phone, no internet needed:\n\n"
                    + "📖 Explain: \"What is photosynthesis?\"\n"
                    + "📝 Summarize: \"Summarize the Solar System\"\n"
                    + "🎮 Quiz: \"Quiz me\" or \"Quiz me on Math\"\n"
                    + "📊 Coach: \"What should I study?\"\n"
                    + "💡 Tips: \"Give me study tips\"\n\n"
                    + "I learn from your Library, so add your own notes and I'll understand them instantly!";

    private static final String TIPS =
            "💡 Study tips that really work:\n\n"
                    + "1. Active recall — close your notes and try to answer from memory (that's what quizzes are for!).\n"
                    + "2. Spaced repetition — review a topic after 1 day, 3 days, then a week.\n"
                    + "3. Pomodoro — 25 minutes focused, 5 minutes break.\n"
                    + "4. Teach it — explain the topic out loud as if to a friend.\n"
                    + "5. Mix subjects — alternating topics helps your brain remember longer.\n\n"
                    + "Want to start now? Say \"quiz me\"!";

    private static boolean has(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) {
                return true;
            }
        }
        return false;
    }

    public static Answer reply(String input, List<Reviewer> revs, List<QuizResult> results,
                               Map<Long, String> names) {
        String q = input == null ? "" : input.trim();
        String lower = q.toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return new Answer("Ask me anything about your reviewers! 📚");
        }
        if (lower.length() < 25 && lower.matches("^(hi|hello|hey|yo|good morning|good afternoon|good evening)\\b.*")) {
            return new Answer(pick(GREETINGS));
        }
        if (lower.equals("help") || has(lower, "what can you do", "who are you")) {
            return new Answer(CAPABILITIES);
        }
        if (has(lower, "quiz me", "test me", "give me a quiz") || lower.startsWith("quiz")) {
            long sid = 0;
            for (Map.Entry<Long, String> e : names.entrySet()) {
                if (lower.contains(e.getValue().toLowerCase(Locale.ROOT))) {
                    sid = e.getKey();
                }
            }
            String where = sid == 0 ? "all your reviewers" : names.get(sid);
            return new Answer("Let's go! 🎮 Here comes a 5-question quiz on " + where + ". Good luck!",
                    Action.QUIZ, sid);
        }
        if (has(lower, "what should i study", "what to study", "my progress", "weak", "recommend", "how am i doing")) {
            return advise(results, names);
        }
        if (has(lower, "tip", "how to study", "study plan", "memorize", "remember better")) {
            return new Answer(TIPS);
        }
        if (lower.startsWith("summar") || has(lower, "summary of", "summarize", "summarise")) {
            String topic = lower.replaceAll("summar\\w*|summary|\\bof\\b|\\bthe\\b|\\bnotes?\\b|\\breviewer\\b", " ").trim();
            Reviewer r = findReviewer(topic, revs);
            if (r == null) {
                if (revs.size() == 1) {
                    r = revs.get(0);
                } else {
                    return new Answer("Which reviewer should I summarize? 🤔 I have: " + titles(revs) + ".");
                }
            }
            return new Answer("📝 Summary of \"" + r.title + "\":\n\n" + summarize(r.content, 4)
                    + "\n\nSay \"quiz me\" when you're ready to test yourself!");
        }

        List<Sent> hits = top(q, revs, 3);
        if (hits.isEmpty()) {
            return new Answer("Hmm, I couldn't find that in your reviewers 🤔\n\n"
                    + "Try rephrasing, or add notes about it in the Library tab and I'll learn them instantly.\n\n"
                    + "Topics I know: " + titles(revs) + ".");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(pick(new String[]{"📖 Here's what your notes say:", "Good question! 💡 From your reviewers:",
                "Here's what I found:"})).append("\n\n");
        Set<String> sources = new LinkedHashSet<>();
        for (Sent s : hits) {
            sb.append("• ").append(s.text).append("\n");
            sources.add(s.title);
        }
        sb.append("\nSource: ").append(join(sources)).append(".\nWant to test yourself? Say \"quiz me\"!");
        return new Answer(sb.toString());
    }

    private static Answer advise(List<QuizResult> results, Map<Long, String> names) {
        if (results == null || results.isEmpty()) {
            return new Answer("You haven't taken any quizzes yet 🌱\n\nStart with a quick one — say \"quiz me\" — "
                    + "and I'll tell you where to focus next.");
        }
        Map<Long, int[]> perSubject = new HashMap<>();
        for (QuizResult r : results) {
            if (r.subjectId == 0) {
                continue;
            }
            int[] a = perSubject.get(r.subjectId);
            if (a == null) {
                a = new int[2];
                perSubject.put(r.subjectId, a);
            }
            a[0] += r.score;
            a[1] += r.total;
        }
        if (perSubject.isEmpty()) {
            int s = 0;
            int t = 0;
            for (QuizResult r : results) {
                s += r.score;
                t += r.total;
            }
            int pct = t == 0 ? 0 : Math.round(100f * s / t);
            return new Answer("📊 Across your last " + results.size() + " quizzes you're getting " + pct
                    + "% right.\n\nTry a subject-specific quiz (e.g. \"quiz me on Math\") so I can pinpoint your weak spots.");
        }
        long weakId = 0;
        long strongId = 0;
        float weak = 2f;
        float strong = -1f;
        for (Map.Entry<Long, int[]> e : perSubject.entrySet()) {
            int[] a = e.getValue();
            float acc = a[1] == 0 ? 0 : (float) a[0] / a[1];
            if (acc < weak) {
                weak = acc;
                weakId = e.getKey();
            }
            if (acc > strong) {
                strong = acc;
                strongId = e.getKey();
            }
        }
        String weakName = names.containsKey(weakId) ? names.get(weakId) : "that subject";
        String strongName = names.containsKey(strongId) ? names.get(strongId) : "that subject";
        StringBuilder sb = new StringBuilder("📊 Based on your recent quizzes:\n\n");
        sb.append("💪 Strongest: ").append(strongName).append(" (").append(Math.round(strong * 100)).append("%)\n");
        if (weakId != strongId) {
            sb.append("🎯 Needs work: ").append(weakName).append(" (").append(Math.round(weak * 100)).append("%)\n\n");
            sb.append("Plan: re-read the ").append(weakName).append(" reviewers, then take a quick quiz. Starting one now!");
        } else {
            sb.append("\nKeep going — a few more quizzes and I'll have a clearer picture. Starting one now!");
        }
        return new Answer(sb.toString(), Action.QUIZ, weakId);
    }

    private static Reviewer findReviewer(String topic, List<Reviewer> revs) {
        List<String> tt = tokens(topic);
        if (tt.isEmpty()) {
            return null;
        }
        Reviewer best = null;
        int bestScore = 0;
        for (Reviewer r : revs) {
            Set<String> title = new HashSet<>(tokens(r.title));
            Set<String> content = new HashSet<>(tokens(r.content));
            int sc = 0;
            for (String t : tt) {
                if (title.contains(t)) {
                    sc += 3;
                }
                if (content.contains(t)) {
                    sc += 1;
                }
            }
            if (sc > bestScore) {
                bestScore = sc;
                best = r;
            }
        }
        return best;
    }

    private static String titles(List<Reviewer> revs) {
        List<String> t = new ArrayList<>();
        for (Reviewer r : revs) {
            t.add(r.title);
        }
        return t.isEmpty() ? "none yet" : join(t);
    }

    private static String join(java.util.Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
