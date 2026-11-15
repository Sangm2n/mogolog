package com.team.mogolog.model;

import com.google.firebase.firestore.DocumentId;

/*
 * [User] 사용자 정보 (users 컬렉션, 문서 이름 = 로그인 uid)
 *
 * 하는 일: 회원가입 때 입력한 닉네임, 선택과목, 익명 통계 동의 여부를 담는다.
 *
 * 누가 언제 쓰나
 *  - 이서준: 회원가입(A2)에서 가입이 끝나면 User를 만들어 저장
 *  - 이서준: 커뮤니티(A9)에서 글쓴이 닉네임을 보여줄 때 읽기
 *  - 이상민: 성적 입력(A6)에서 내 선택과목을 미리 골라줄 때 읽기
 *  - 이상민: 성적 저장 시 statsAgreed 값을 Score.statsIncluded에 복사
 */
public class User {
    @DocumentId
    private String uid;              // 문서 이름이 자동으로 들어감

    private String nickname;         // 닉네임
    private String koreanElective;   // 국어 선택과목 (예: "언어와매체")
    private String mathElective;     // 수학 선택과목 (예: "미적분")
    private String inquiry1;         // 탐구 1 (예: "물리학1")
    private String inquiry2;         // 탐구 2 (예: "화학1")
    // ※ 선택과목 값은 Subjects 목록에 있는 글자 그대로 저장
    private boolean statsAgreed;     // 익명 통계 동의 여부

    // Firestore가 데이터를 읽어올 때 필요 (지우지 말 것)
    public User() {}

    // ===== getter / setter (Firestore가 저장·읽기에 사용) =====
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getKoreanElective() { return koreanElective; }
    public void setKoreanElective(String koreanElective) { this.koreanElective = koreanElective; }

    public String getMathElective() { return mathElective; }
    public void setMathElective(String mathElective) { this.mathElective = mathElective; }

    public String getInquiry1() { return inquiry1; }
    public void setInquiry1(String inquiry1) { this.inquiry1 = inquiry1; }

    public String getInquiry2() { return inquiry2; }
    public void setInquiry2(String inquiry2) { this.inquiry2 = inquiry2; }

    public boolean isStatsAgreed() { return statsAgreed; }
    public void setStatsAgreed(boolean statsAgreed) { this.statsAgreed = statsAgreed; }
}
