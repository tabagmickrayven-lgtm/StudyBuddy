package com.studybuddy.app.data;

import android.content.Context;

import com.studybuddy.app.ai.LocalAI;

import java.util.ArrayList;
import java.util.List;

/** Fills the database with starter subjects, reviewers and questions on first launch. */
public final class Seeder {

    private Seeder() {
    }

    private static final String CELLS =
            "Photosynthesis is the process by which plants convert sunlight, water, and carbon dioxide into glucose and oxygen. "
                    + "It takes place mainly in the chloroplasts, which contain a green pigment called chlorophyll. "
                    + "Chlorophyll absorbs light energy, mostly from the red and blue parts of the spectrum. "
                    + "The cell is the basic unit of life, and every living organism is made of one or more cells. "
                    + "The nucleus controls the activities of the cell and stores its DNA. "
                    + "Mitochondria are known as the powerhouse of the cell because they release energy through cellular respiration. "
                    + "The cell membrane controls what enters and leaves the cell. "
                    + "Plant cells have a rigid cell wall made of cellulose, which animal cells do not have. "
                    + "Ribosomes build proteins inside the cell.";

    private static final String SOLAR =
            "The Solar System formed about 4.6 billion years ago from a giant cloud of gas and dust. "
                    + "The Sun contains more than 99 percent of the total mass of the Solar System. "
                    + "Mercury is the closest planet to the Sun, while Neptune is the farthest. "
                    + "Jupiter is the largest planet and has a famous storm called the Great Red Spot. "
                    + "Venus is the hottest planet because its thick atmosphere traps heat. "
                    + "Saturn is known for its bright rings made of ice and rock. "
                    + "Mars is called the Red Planet because of the iron oxide on its surface. "
                    + "Earth is the only known planet that supports life. "
                    + "The Moon takes about 27 days to orbit the Earth.";

    private static final String MATH =
            "A fraction represents a part of a whole, written as a numerator over a denominator. "
                    + "To add fractions with different denominators, first find a common denominator. "
                    + "A percent means per hundred, so 50 percent is equal to one half. "
                    + "To find a percent of a number, multiply the number by the percent and divide by 100. "
                    + "The perimeter of a rectangle is two times the sum of its length and width. "
                    + "The area of a rectangle is length multiplied by width. "
                    + "The area of a triangle is one half times the base times the height. "
                    + "The area of a circle is pi times the radius squared. "
                    + "The sum of the interior angles of a triangle is always 180 degrees. "
                    + "The Pythagorean theorem states that in a right triangle, a squared plus b squared equals c squared.";

    private static final String HISTORY =
            "Ferdinand Magellan reached the Philippines in 1521 and was killed in the Battle of Mactan by the forces of Lapulapu. "
                    + "Spain colonized the Philippines for more than 300 years, beginning with the expedition of Miguel Lopez de Legazpi in 1565. "
                    + "The Katipunan was a revolutionary society founded by Andres Bonifacio in 1892. "
                    + "Jose Rizal wrote the novels Noli Me Tangere and El Filibusterismo to expose the abuses of colonial rule. "
                    + "Rizal was executed by firing squad on December 30, 1896, in Bagumbayan. "
                    + "Emilio Aguinaldo declared Philippine independence on June 12, 1898, in Kawit, Cavite. "
                    + "The Philippines was a colony of the United States from 1898 until 1946. "
                    + "The People Power Revolution of 1986 along EDSA ended the rule of Ferdinand Marcos.";

    private static final String ENGLISH =
            "A noun is a word that names a person, place, thing, or idea. "
                    + "A verb is a word that expresses an action or a state of being. "
                    + "An adjective describes or modifies a noun or pronoun. "
                    + "An adverb modifies a verb, an adjective, or another adverb, and it often ends in the letters ly. "
                    + "A pronoun takes the place of a noun, such as she, they, or it. "
                    + "A preposition shows the relationship between a noun and another word in the sentence. "
                    + "A conjunction joins words, phrases, or clauses, such as and, but, or or. "
                    + "A complete sentence must contain a subject and a predicate. "
                    + "The subject and verb of a sentence must agree in number.";

    private static Question q(long sid, long rid, String text, String a, String b, String c, String d,
                              int correct, String explanation) {
        Question q = new Question();
        q.subjectId = sid;
        q.reviewerId = rid;
        q.question = text;
        q.a = a;
        q.b = b;
        q.c = c;
        q.d = d;
        q.correct = correct;
        q.explanation = explanation;
        q.source = 0;
        return q;
    }

    public static void ensureSeeded(Context context) {
        StudyDao dao = AppDatabase.get(context).dao();
        if (dao.subjectCount() > 0) {
            return;
        }

        long sci = dao.insertSubject(new Subject("Science", "🔬"));
        long math = dao.insertSubject(new Subject("Math", "📐"));
        long hist = dao.insertSubject(new Subject("History", "🏛️"));
        long eng = dao.insertSubject(new Subject("English", "📚"));

        long cells = dao.insertReviewer(new Reviewer(sci, "Photosynthesis & Cells", CELLS));
        long solar = dao.insertReviewer(new Reviewer(sci, "The Solar System", SOLAR));
        long mathR = dao.insertReviewer(new Reviewer(math, "Fractions, Percent & Geometry", MATH));
        long histR = dao.insertReviewer(new Reviewer(hist, "Philippine History Highlights", HISTORY));
        long engR = dao.insertReviewer(new Reviewer(eng, "Parts of Speech & Sentences", ENGLISH));

        List<Question> qs = new ArrayList<>();
        qs.add(q(sci, cells, "Which organelle is known as the powerhouse of the cell?",
                "Nucleus", "Mitochondria", "Ribosome", "Chloroplast", 1,
                "Mitochondria release energy through cellular respiration."));
        qs.add(q(sci, cells, "Which pigment in plants absorbs light energy for photosynthesis?",
                "Melanin", "Chlorophyll", "Hemoglobin", "Cellulose", 1,
                "Chlorophyll is the green pigment in chloroplasts."));
        qs.add(q(sci, cells, "Which structure is found in plant cells but NOT in animal cells?",
                "Mitochondria", "Nucleus", "Cell wall", "Cell membrane", 2,
                "Plant cells have a rigid cell wall made of cellulose."));
        qs.add(q(sci, solar, "Which is the largest planet in the Solar System?",
                "Saturn", "Jupiter", "Neptune", "Earth", 1,
                "Jupiter is the largest planet and has the Great Red Spot."));
        qs.add(q(sci, solar, "Which planet is called the Red Planet?",
                "Venus", "Mercury", "Mars", "Jupiter", 2,
                "Iron oxide on its surface gives Mars its red color."));
        qs.add(q(sci, solar, "Which planet is closest to the Sun?",
                "Mercury", "Venus", "Earth", "Mars", 0,
                "Mercury is the closest planet to the Sun."));
        qs.add(q(math, mathR, "What is the sum of the interior angles of a triangle?",
                "90 degrees", "180 degrees", "270 degrees", "360 degrees", 1,
                "The interior angles of any triangle always add up to 180 degrees."));
        qs.add(q(math, mathR, "What is 25 percent of 80?",
                "15", "20", "25", "40", 1,
                "80 x 25 / 100 = 20."));
        qs.add(q(math, mathR, "A rectangle has length 8 and width 5. What is its area?",
                "13", "26", "40", "45", 2,
                "Area = length x width = 8 x 5 = 40."));
        qs.add(q(math, mathR, "In a right triangle with legs 3 and 4, what is the hypotenuse?",
                "5", "6", "7", "12", 0,
                "3 squared + 4 squared = 9 + 16 = 25, and the square root of 25 is 5."));
        qs.add(q(hist, histR, "Who declared Philippine independence on June 12, 1898?",
                "Andres Bonifacio", "Emilio Aguinaldo", "Jose Rizal", "Apolinario Mabini", 1,
                "Emilio Aguinaldo declared independence in Kawit, Cavite."));
        qs.add(q(hist, histR, "Who founded the Katipunan?",
                "Emilio Jacinto", "Andres Bonifacio", "Marcelo H. del Pilar", "Antonio Luna", 1,
                "Andres Bonifacio founded the Katipunan in 1892."));
        qs.add(q(hist, histR, "Which of these is a novel written by Jose Rizal?",
                "Florante at Laura", "Noli Me Tangere", "Banaag at Sikat", "Dekada '70", 1,
                "Rizal wrote Noli Me Tangere and El Filibusterismo."));
        qs.add(q(eng, engR, "Which part of speech describes a noun?",
                "Verb", "Adverb", "Adjective", "Conjunction", 2,
                "An adjective describes or modifies a noun or pronoun."));
        qs.add(q(eng, engR, "Which of these words is a conjunction?",
                "quickly", "but", "happy", "under", 1,
                "Conjunctions such as and, but, or join words, phrases, or clauses."));
        qs.add(q(eng, engR, "A complete sentence must have a ____ and a predicate.",
                "subject", "adverb", "pronoun", "phrase", 0,
                "Every complete sentence needs a subject and a predicate."));
        dao.insertQuestions(qs);

        // Let the on-device AI generate extra questions from each reviewer.
        List<Reviewer> all = dao.reviewers();
        List<String> pool = LocalAI.termPool(all);
        for (Reviewer r : all) {
            dao.insertQuestions(LocalAI.generateQuestions(r, 6, pool));
        }
    }
}
