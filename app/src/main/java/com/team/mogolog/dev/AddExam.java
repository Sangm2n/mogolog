package com.team.mogolog.dev;

import com.google.firebase.firestore.FirebaseFirestore;
import com.team.mogolog.model.Exam;
import com.team.mogolog.model.FirestorePaths;
import com.team.mogolog.model.Subjects;

import java.util.ArrayList;
import java.util.Arrays;
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

    // 전 과목 목록 (Subjects 배열로 만들기)
    private static List<String> allSubjects() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, Subjects.COMMON);
        Collections.addAll(list, Subjects.SOCIAL_INQUIRY);
        Collections.addAll(list, Subjects.SCIENCE_INQUIRY);
        return list;
    }

    // 시험 데이터 넣기 -> 필요할 때마다 함수 본문 직접 수정
    public static void seed() {
        FirebaseFirestore db = FirebaseFirestore.getInstance(); //Firestore 연결하기
        List<String> all = allSubjects(); // 전과목 목록 all에 저장하기

        // 평가원·교육청
        addExamToFirestore(db, "2026-03-edu", "2026년 3월 학력평가", "교육청", null, "2026-03-24", all);
        addExamToFirestore(db, "2026-05-edu", "2026년 5월 학력평가", "교육청", null, "2026-05-07", all);
        addExamToFirestore(db, "2026-06-kice", "2027학년도 6월 모의평가", "평가원", null, "2026-06-04", all);
        addExamToFirestore(db, "2026-07-edu", "2026년 7월 학력평가", "교육청", null, "2026-07-08", all);
        addExamToFirestore(db, "2026-09-kice", "2027학년도 9월 모의평가", "평가원", null, "2026-09-02", all);
        addExamToFirestore(db, "2026-10-edu", "2026년 10월 학력평가", "교육청", null, "2026-10-20", all);
        addExamToFirestore(db, "2026-11-kice", "2027학년도 대학수학능력시험", "평가원", null, "2026-11-19", all);

        // 사설 (날짜는 시연용)
        addExamToFirestore(db, "2026-04-daesung-premium", "대성마이맥 더프리미엄 모의고사 4월", "사설", "대성마이맥", "2026-04-16", all);
        addExamToFirestore(db, "2026-08-sidae-survival-1", "시대인재 서바이벌 1회", "사설", "시대인재", "2026-08-15", all);
        addExamToFirestore(db, "2026-08-mega-killingcamp-1", "메가스터디 킬링캠프 1회", "사설", "메가스터디", "2026-08-22", Arrays.asList("수학"));
        addExamToFirestore(db, "2026-09-sidae-survival-2", "시대인재 서바이벌 2회", "사설", "시대인재", "2026-09-19", all);
        addExamToFirestore(db, "2026-10-daesung-premium", "대성마이맥 더프리미엄 모의고사 10월", "사설", "대성마이맥", "2026-10-08", all);

        // 미래 시험 (D-day 시연용, 가상 날짜)
        addExamToFirestore(db, "2027-03-edu", "2027년 3월 학력평가", "교육청", null, "2027-03-23", all);
    }



}
