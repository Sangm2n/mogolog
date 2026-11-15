package com.team.mogolog.model;

/*
 * [Subjects] 과목 이름 표준 목록 (개편 전 수능 기준)
 *
 * 하는 일: Firestore에 저장하는 과목 이름을 한 곳에 정해 둔다.
 *          띄어쓰기·특수문자 없이 이 목록의 글자 그대로만 저장한다.
 *          (예: "언어와매체", "물리학1" / "언어와 매체", "물리학Ⅰ"은 다른 값이 되므로 쓰지 않기)
 *
 * 누가 언제 쓰나
 *  - 이서준: 회원가입(A2)의 선택과목 칩 → KOREAN_ELECTIVES, MATH_ELECTIVES, SOCIAL_INQUIRY, SCIENCE_INQUIRY
 *  - 이상민: 시험 등록(B4)의 Exam.subjects, 성적 입력(A6)의 Score.subject / Score.elective
 *  - 전원: 과목 이름이 필요할 때 직접 입력하지 말고 이 목록에서 꺼내 쓰기
 */
public class Subjects {
    // 공통 과목
    public static final String[] COMMON = {"국어", "수학", "영어", "한국사"};

    // 국어 선택과목
    public static final String[] KOREAN_ELECTIVES = {"화법과작문", "언어와매체"};

    // 수학 선택과목
    public static final String[] MATH_ELECTIVES = {"확률과통계", "미적분", "기하"};

    // 사회탐구
    public static final String[] SOCIAL_INQUIRY = {
            "생활과윤리", "윤리와사상", "한국지리", "세계지리", "동아시아사",
            "세계사", "경제", "정치와법", "사회문화"
    };

    // 과학탐구
    public static final String[] SCIENCE_INQUIRY = {
            "물리학1", "화학1", "생명과학1", "지구과학1",
            "물리학2", "화학2", "생명과학2", "지구과학2"
    };
}
