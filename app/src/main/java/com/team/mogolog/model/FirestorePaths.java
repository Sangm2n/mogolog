package com.team.mogolog.model;

/*
 * [FirestorePaths] 여러 명이 같이 쓰는 이름 모음 (오타 방지용)
 *
 * 하는 일: 컬렉션 이름과 화면 이동 때 넘기는 값의 이름을 한 곳에 모아 둔다.
 *          "scores"처럼 글자를 직접 쓰지 말고 이 상수를 쓰면 오타가 나도 빨간 줄로 알려준다.
 *
 * 누가 언제 쓰나
 *  - 전원: Firestore를 쓸 때  예) db.collection(FirestorePaths.SCORES)
 *  - 이상민 → 안재한: 시험 상세(A7)에서 문항 상세(A8)로 이동할 때 시험 id·과목·문항 번호 전달
 *  - 안재한 → 이서준: 문항 상세(A8)에서 커뮤니티(A9)로 이동할 때 같은 값 전달
 *      보내기: intent.putExtra(FirestorePaths.EXTRA_EXAM_ID, examId);
 *      받기:   getIntent().getStringExtra(FirestorePaths.EXTRA_EXAM_ID);
 */
public class FirestorePaths {
    // 컬렉션 이름
    public static final String USERS = "users";
    public static final String EXAMS = "exams";
    public static final String SCORES = "scores";
    public static final String NOTES = "notes";
    public static final String SELECTIONS = "selections";   // 시험별로 고른 선택과목 (문서 이름 = uid_시험id)

    // 화면 이동할 때 Intent로 넘기는 값의 이름
    public static final String EXTRA_EXAM_ID = "examId";                  // String
    public static final String EXTRA_SUBJECT = "subject";                 // String
    public static final String EXTRA_QUESTION_NUMBER = "questionNumber";  // int
}
