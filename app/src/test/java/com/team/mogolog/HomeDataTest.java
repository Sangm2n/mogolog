package com.team.mogolog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.team.mogolog.model.Exam;
import com.team.mogolog.model.Note;
import com.team.mogolog.model.Score;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HomeDataTest {
    @Test
    public void nearestExamIncludesTodayAndIgnoresInvalidDates() {
        Exam today = exam("today", "2026-11-22");
        HomeData data = new HomeData(null, Arrays.asList(exam("future", "2026-11-27"),
                today, exam("invalid", "2026-11-31"), exam("past", "2026-11-21")),
                Collections.emptyList(), Collections.emptyList(), true, false, false, false);
        assertEquals(today, data.nextExam(HomeData.dayNumber("2026-11-22")));
        assertEquals(Long.MIN_VALUE, HomeData.dayNumber("2026-11-31"));
    }

    @Test
    public void chartUsesOwnSubjectAndOrdersByExamDate() {
        Score early = score("user", "first", "수학");
        Score recent = score("user", "latest", "수학");
        HomeData data = new HomeData("user",
                Arrays.asList(exam("latest", "2026-11-22"), exam("first", "2026-02-01")),
                Arrays.asList(recent, score("other", "latest", "수학"), early,
                        score("user", "first", "영어"), score("user", "missing", "수학")),
                Collections.emptyList(), true, true, true, false);
        assertEquals(Arrays.asList(early, recent), data.datedScores("수학"));
    }

    @Test
    public void localCompletionOverridesServerAndOnlyWrongQuestionsAreCountedOnce() {
        Score score = score("user", "exam", "수학");
        score.setWrongNumbers(Arrays.asList(14, 14, 21, 29, null, 0));
        Note confirmed = note(14);
        Note correctQuestionNote = note(3);
        HomeData data = new HomeData("user", Collections.emptyList(), Collections.singletonList(score),
                Arrays.asList(confirmed, correctQuestionNote), true, true, true, false);

        assertTrue(data.reviewItems(id -> null).get(0).done);
        List<HomeData.ReviewItem> items = data.reviewItems(id -> {
            if (id.equals("user_exam_수학_14")) {
                return false;
            }
            return id.equals("user_exam_수학_21") ? Boolean.TRUE : null;
        });
        assertEquals(3, items.size());
        assertFalse(items.get(0).done);
        assertTrue(items.get(1).done);
        assertFalse(items.get(2).done);
    }

    private static Exam exam(String id, String date) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setDate(date);
        return exam;
    }

    private static Score score(String owner, String exam, String subject) {
        Score score = new Score();
        score.setUid(owner);
        score.setExamId(exam);
        score.setSubject(subject);
        return score;
    }

    private static Note note(int number) {
        Note note = new Note();
        note.setUid("user");
        note.setExamId("exam");
        note.setSubject("수학");
        note.setQuestionNumber(number);
        note.setDone(true);
        return note;
    }
}
