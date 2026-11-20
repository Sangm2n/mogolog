package com.team.mogolog.dev;

import com.google.firebase.firestore.FirebaseFirestore;
import com.team.mogolog.model.Exam;
import com.team.mogolog.model.FirestorePaths;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddExam {

    // 각 과목 별 문항 수
    private static int questionCount(String subject) {
        if (subject.equals("국어") || subject.equals("영어"))
            return 45;
        else if (subject.equals("수학"))
            return 30;
        else
            return 20;
    }

    // Exam 객체를 만들어 Firestore에 저장
    private static void addExamToFirestore(FirebaseFirestore db, String id, String name, String type,
                                    String provider, String date, List<String> subjects) {
        Exam exam = new Exam();

        // exam 객체에 시험 정보 넣기
        exam.setName(name);
        exam.setType(type);
        exam.setProvider(provider);
        exam.setDate(date);
        exam.setSubjects(subjects);

        Map<String, Integer> questionCountsMap = new HashMap<>();
        for (String s : subjects)
            questionCountsMap.put(s, questionCount(s));
        exam.setQuestionCounts(questionCountsMap);

        // Firestore에 저장
        db.collection(FirestorePaths.EXAMS).document(id).set(exam);
    }




}
