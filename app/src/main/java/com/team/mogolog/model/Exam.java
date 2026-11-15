package com.team.mogolog.model;

import com.google.firebase.firestore.DocumentId;

import java.util.List;
import java.util.Map;

/*
 * [Exam] 시험 정보 (exams 컬렉션, 문서 이름 예: "2026-06-kice")
 *
 * 하는 일: 관리자가 미리 등록해 두는 모의고사 목록. 사용자는 읽기만 한다.
 *          시험 이름, 종류(평가원/교육청/사설), 날짜, 과목, 과목별 문항 수를 담는다.
 *
 * 누가 언제 쓰나
 *  - 이상민: 시연용 시험 등록(B4) 때 저장
 *  - 이상민: 시험 기록(A5) 목록과 종류 필터, 성적 입력(A6)의 시험·과목 선택, 틀린 번호 칸 수
 *  - 안재한: 홈(A4)에서 다음 모의고사 D-day 계산
 *  - 이서준: 커뮤니티(A9)의 시험·과목 선택, 알림(A13)의 시험 날짜
 */
public class Exam {
    @DocumentId
    private String id;                           // 문서 이름이 자동으로 들어감

    private String name;                         // 시험 이름 (예: "2026년 6월 모의평가")
    private String type;                         // "평가원" / "교육청" / "사설" 중 하나
    private String provider;                     // 사설 출제처 (예: "시대인재"), 평가원·교육청은 null
    private String date;                         // 시험 날짜 (예: "2026-06-04")
    private List<String> subjects;               // 과목 목록 (예: ["국어", "수학", "영어"])
    private Map<String, Integer> questionCounts; // 과목별 문항 수 (예: {"국어": 45, "수학": 30, "영어": 45})

    // Firestore가 데이터를 읽어올 때 필요
    public Exam() {}

    // ===== getter / setter (Firestore가 저장·읽기에 사용) =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }

    public Map<String, Integer> getQuestionCounts() { return questionCounts; }
    public void setQuestionCounts(Map<String, Integer> questionCounts) { this.questionCounts = questionCounts; }
}
