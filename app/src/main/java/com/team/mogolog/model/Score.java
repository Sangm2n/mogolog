package com.team.mogolog.model;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

/*
 * [Score] 성적 (scores 컬렉션, 문서 이름 = uid_시험id_과목)
 *
 * 하는 일: 한 사람의 한 시험, 한 과목 성적. 원점수, 등급, 틀린 번호를 담는다.
 *          문서 이름은 꼭 makeId()로 만든다. (같은 시험·과목 성적이 한 사람당 하나만 생김)
 *
 * 누가 언제 쓰나
 *  - 이상민: 성적 입력(A6)에서 저장
 *  - 이상민: 시험 기록(A5), 시험 상세(A7), 통계 계산(B6)에서 읽기
 *            (통계는 같은 examId·subject·elective이고 statsIncluded가 true인 성적만 사용)
 *  - 안재한: 홈(A4)의 최근 시험 요약, 원점수 그래프, 틀린 문제 수에서 읽기
 */
public class Score {
    @DocumentId
    private String id;                   // 문서 이름이 자동으로 들어감

    private String uid;                  // 누구의 성적인지
    private String examId;               // 어떤 시험인지 (Exam의 id)
    private String subject;              // 과목 (예: "수학", "물리학1") - Subjects 목록의 값
    private String elective;             // 국어·수학 선택과목 (예: "미적분"), 없으면 null
    private int rawScore;                // 원점수
    private Integer grade;               // 등급, 입력 안 하면 null
    private List<Integer> wrongNumbers;  // 틀린 번호 (예: [12, 21, 29])
    private boolean statsIncluded;       // 통계에 포함할지 (저장할 때 User의 statsAgreed 값을 넣기)

    // Firestore가 데이터를 읽어올 때 필요 (지우지 말 것)
    public Score() {}

    // ===== getter / setter (Firestore가 저장·읽기에 사용) =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getElective() { return elective; }
    public void setElective(String elective) { this.elective = elective; }

    public int getRawScore() { return rawScore; }
    public void setRawScore(int rawScore) { this.rawScore = rawScore; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public List<Integer> getWrongNumbers() { return wrongNumbers; }
    public void setWrongNumbers(List<Integer> wrongNumbers) { this.wrongNumbers = wrongNumbers; }

    public boolean isStatsIncluded() { return statsIncluded; }
    public void setStatsIncluded(boolean statsIncluded) { this.statsIncluded = statsIncluded; }

    // 문서 이름 만들기. 예: makeId("abc123", "2026-06-kice", "수학") → "abc123_2026-06-kice_수학"
    public static String makeId(String uid, String examId, String subject) {
        return uid + "_" + examId + "_" + subject;
    }
}
