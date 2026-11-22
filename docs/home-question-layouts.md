# 홈 / 문항 상세 레이아웃

## 이번 작업 범위

- 홈: A4-1 레이아웃, A4-2 D-day 카드, A4-3 미정리 문제 수,
  A4-4 진행률, A4-5 최근 시험 요약의 화면 구성.
- 문항 상세: A8-1 레이아웃. 사진 영역, 문항 배지, 코멘트 입력칸,
  완료 토글, 질문 보기 버튼을 배치합니다.
- 현재 로그인 계정으로 Firestore의 시험·성적·오답 데이터를 조회합니다.
- 홈의 정리하기는 홈 전용 오답 목록을 엽니다. 문항을 눌러 상세를 열 수 있습니다.
  목록과 문항 상세는 홈 안에서 관리하므로 기록·커뮤니티 탭의 화면이나 데이터를 사용하지 않습니다.
- 원점수 추이(A4-6)는 국어·수학·영어 선택과 꺾은선 차트로 구성합니다.
  차트의 예시 점수는 Android Studio 미리보기에 표시합니다.
  실행 시 점수 데이터가 비어 있으면 빈 상태를 표시합니다.

## 미리보기와 실행 화면

Android Studio에서 아래 파일을 열고 **Design** 또는 **Split**을 선택합니다.

- `app/src/main/res/layout/fragment_home.xml`
- `app/src/main/res/layout/activity_question_detail.xml`

`tools:text`, `tools:progress`, `tools:checked`는 제공된 Figma 스크린샷의 예시입니다.
디버그 실행에서 홈 하단의 **예시 데이터 보기**를 선택하면 로그인이 없어도 D-day,
최근 시험, 과목 전환, 오답 목록, 문항 상세의 흐름을 확인할 수 있습니다.
예시 데이터의 변경은 실제 계정의 변경과 별개로 저장됩니다.

문항 상세는 `QuestionDetailFragment`로 열립니다. 카메라 앱이 반환하는 축소 사진과
코멘트·완료 상태는 계정과 문항별로 기기에 저장합니다. 완료 토글은 홈과 홈 전용 오답 목록에 즉시 반영합니다.
질문 보기 버튼은 디자인 미리보기에 남겨두며, 커뮤니티 담당 화면과 연결할 때 표시합니다.

## 데이터 연결용 View ID

| 홈 영역 | View ID |
| --- | --- |
| 시험 이름 / 종류 / D-day | `home_next_exam_name`, `home_next_exam_badge`, `home_next_exam_day_count` |
| 미정리 문제 수 | `home_unreviewed_count` |
| 진행률 문구 / 막대 | `home_review_progress_value`, `home_review_progress` |
| 최근 시험명 | `home_recent_exam_name` |
| 과목별 점수 | `home_korean_score`, `home_math_score`, `home_english_score`, `home_inquiry_score` |
| 과목명 | 위 점수 ID의 `_score`를 `_subject`로 바꾼 ID |
| 원점수 과목 선택 / 차트 | `home_trend_subjects`, `home_score_trend_chart` |
| 버튼 | `home_notifications`, `home_review_action` |

| 문항 상세 영역 | View ID |
| --- | --- |
| 제목 / 뒤로 | `question_title`, `question_back` |
| 사진 / 안내 영역 / 촬영 버튼 | `question_photo`, `question_photo_placeholder`, `question_capture_photo` |
| 오답 / 오답률 / 선택과목 | `question_wrong_badge`, `question_error_rate`, `question_elective` |
| 코멘트 / 완료 토글 | `question_comment`, `question_review_done` |
| 커뮤니티 이동 | `question_community_action` |

기존 공통 색상·글자 스타일·카드 모서리·화면 여백을 재사용합니다.
화면별 파생 스타일은 `styles_screens.xml`, 문구는 `strings_screens.xml`에 있습니다.
데이터 모델·Firestore 필드·Gradle 설정·Manifest는 변경하지 않습니다.
선택 과목의 점수를 시험 날짜순으로 정렬하고 `ScoreTrendView.setPoints()`에 전달합니다.
차트에는 최근 5건까지 표시합니다.
