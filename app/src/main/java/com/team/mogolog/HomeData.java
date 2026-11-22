package com.team.mogolog;

import com.team.mogolog.model.Exam;
import com.team.mogolog.model.Note;
import com.team.mogolog.model.Score;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;

final class HomeData {
    private static final long MILLIS_PER_DAY = 86_400_000L;

    final String owner;
    final List<Exam> exams;
    final List<Score> scores;
    final boolean examsAvailable;
    final boolean scoresAvailable;
    final boolean notesAvailable;
    final boolean loading;

    private final Map<String, Exam> examsById = new HashMap<>();
    private final Map<String, Note> notesById = new HashMap<>();

    HomeData(String owner, List<Exam> exams, List<Score> scores, List<Note> notes,
            boolean examsAvailable, boolean scoresAvailable, boolean notesAvailable, boolean loading) {
        this.owner = owner;
        this.exams = exams;
        this.scores = scores;
        this.examsAvailable = examsAvailable;
        this.scoresAvailable = scoresAvailable;
        this.notesAvailable = notesAvailable;
        this.loading = loading;
        for (Exam exam : exams) {
            if (exam.getId() != null) {
                examsById.put(exam.getId(), exam);
            }
        }
        if (owner != null) {
            for (Note note : notes) {
                if (owner.equals(note.getUid()) && note.getExamId() != null
                        && note.getSubject() != null && note.getQuestionNumber() > 0) {
                    notesById.put(Note.makeId(owner, note.getExamId(), note.getSubject(),
                            note.getQuestionNumber()), note);
                }
            }
        }
    }

    static HomeData empty(String owner, boolean loading) {
        return new HomeData(owner, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), false, false, false, loading);
    }

    Exam exam(String examId) {
        return examsById.get(examId);
    }

    Note note(String noteId) {
        return notesById.get(noteId);
    }

    Exam nextExam(long today) {
        Exam next = null;
        long nearest = Long.MAX_VALUE;
        for (Exam exam : exams) {
            long day = examDay(exam);
            if (day >= today && day < nearest) {
                nearest = day;
                next = exam;
            }
        }
        return next;
    }

    static long today() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return dayNumber(format.format(new Date()));
    }

    static long dayNumber(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            return Long.MIN_VALUE;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date parsed = format.parse(value, new ParsePosition(0));
        return parsed == null ? Long.MIN_VALUE : Math.floorDiv(parsed.getTime(), MILLIS_PER_DAY);
    }

    private static long examDay(Exam exam) {
        return exam == null ? Long.MIN_VALUE : dayNumber(exam.getDate());
    }

    private boolean isOwnScore(Score score) {
        return owner != null && owner.equals(score.getUid()) && score.getExamId() != null
                && score.getSubject() != null;
    }

    List<Score> datedScores(String subject) {
        List<Score> result = new ArrayList<>();
        for (Score score : scores) {
            if (isOwnScore(score) && (subject == null || subject.equals(score.getSubject()))
                    && examDay(exam(score.getExamId())) != Long.MIN_VALUE) {
                result.add(score);
            }
        }
        result.sort(Comparator.comparingLong((Score score) -> examDay(exam(score.getExamId())))
                .thenComparing(Score::getExamId).thenComparing(Score::getSubject));
        return result;
    }

    List<ReviewItem> reviewItems(Function<String, Boolean> localDone) {
        Map<String, ReviewItem> items = new LinkedHashMap<>();
        for (Score score : scores) {
            if (!isOwnScore(score) || score.getWrongNumbers() == null) {
                continue;
            }
            for (Integer number : score.getWrongNumbers()) {
                if (number == null || number <= 0) {
                    continue;
                }
                String id = Note.makeId(owner, score.getExamId(), score.getSubject(), number);
                if (items.containsKey(id)) {
                    continue;
                }
                Note note = note(id);
                Boolean override = localDone.apply(id);
                boolean done = override != null ? override : note != null && note.isDone();
                items.put(id, new ReviewItem(score, number, id, done));
            }
        }
        List<ReviewItem> sorted = new ArrayList<>(items.values());
        sorted.sort(Comparator.comparingLong((ReviewItem item) -> examDay(exam(item.score.getExamId())))
                .reversed().thenComparing(item -> item.score.getExamId())
                .thenComparing(item -> item.score.getSubject()).thenComparingInt(item -> item.number));
        return sorted;
    }

    static final class ReviewItem {
        final Score score;
        final int number;
        final String noteId;
        final boolean done;

        ReviewItem(Score score, int number, String noteId, boolean done) {
            this.score = score;
            this.number = number;
            this.noteId = noteId;
            this.done = done;
        }
    }
}
