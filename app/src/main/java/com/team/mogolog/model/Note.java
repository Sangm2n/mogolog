package com.team.mogolog.model;

import com.google.firebase.firestore.DocumentId;

/*
 * [Note] 문항별 오답 정리 (notes 컬렉션, 문서 이름 = uid_시험id_과목_번호)
 *
 * 하는 일: 문항 하나에 대한 사진, 풀이·코멘트, 정리 완료 여부를 담는다. (맞은 문제도 가능)
 *          문서 이름은 꼭 makeId()로 만든다.
 *
 * 누가 언제 쓰나
 *  - 안재한: 문항 상세(A8)에서 사진·코멘트 저장, 정리 완료 표시
 *  - 안재한: 홈(A4)의 정리 안 한 문제 수, 오답 정리 진행률에서 읽기
 *  - 이상민: 시험 상세(A7)의 문항 목록에서 정리 상태 표시할 때 읽기
 */
public class Note {
    @DocumentId
    private String id;           // 문서 이름이 자동으로 들어감

    private String uid;          // 작성자
    private String examId;       // 시험 (Exam의 id)
    private String subject;      // 과목
    private int questionNumber;  // 문항 번호
    private String photoUrl;     // 문제 사진 주소, 사진 없으면 null
    private String comment;      // 풀이·코멘트
    private boolean done;        // 정리 완료 여부

    // Firestore가 데이터를 읽어올 때 필요 (지우지 말 것)
    public Note() {}

    // ===== getter / setter (Firestore가 저장·읽기에 사용) =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public int getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(int questionNumber) { this.questionNumber = questionNumber; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    // 문서 이름 만들기. 예: makeId("abc123", "2026-06-kice", "수학", 21) → "abc123_2026-06-kice_수학_21"
    public static String makeId(String uid, String examId, String subject, int questionNumber) {
        return uid + "_" + examId + "_" + subject + "_" + questionNumber;
    }
}
