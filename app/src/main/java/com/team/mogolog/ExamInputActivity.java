package com.team.mogolog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.mogolog.databinding.ActivityExamInputBinding;
import com.team.mogolog.databinding.ItemExamInputChipBinding;
import com.team.mogolog.databinding.ItemWrongNumberBinding;
import com.team.mogolog.model.Exam;
import com.team.mogolog.model.FirestorePaths;
import com.team.mogolog.model.Score;
import com.team.mogolog.model.Subjects;
import com.team.mogolog.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamInputActivity extends AppCompatActivity {

    ActivityExamInputBinding binding;
    private String examId; // intent로 받아올 시험 id
    private Exam exam; // 선택한 시험의 Exam 객체
    private String korean = "-"; // 국어 선택과목
    private String math = "-"; // 수학 선택과목
    private List<String> inquiry = new ArrayList<>(); // 탐구 (2과목)

    // 선택한 과목 칩을 만드는데 필요한 두 변수
    private String subject = null;
    private CheckBox[] subjectChips;
    private List<Integer> wrongNumbers = new ArrayList<>(); // 고른 틀린 문항 번호
    private boolean statsAgreed = false; // 통계 동의 여부
    private Map<String, String> rawDraft = new HashMap<>(); // 과목별 입력하던 원점수
    private Map<String, String> gradeDraft = new HashMap<>(); // 과목별 입력하던 등급
    private Map<String, List<Integer>> wrongDraft = new HashMap<>(); // 과목별 고르던 틀린 번호
    private Map<String, Score> saved = new HashMap<>(); // 이 시험에 저장된 내 성적 (과목별)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());

        binding = ActivityExamInputBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        subjectChips = new CheckBox[] {
                binding.subjectKorean, binding.subjectMath, binding.subjectEnglish,
                binding.subjectHistory, binding.subjectInquiry1, binding.subjectInquiry2 };
        for (CheckBox chip : subjectChips) {
            chip.setOnClickListener(v -> selectSubject(chip));
        }

        // 과목을 고르기 전 안내
        binding.examInputElectiveHint.setText("과목을 고르면 문항 번호가 나와요");
        binding.examInputElectiveHint.setVisibility(View.VISIBLE);

        // intent에서 시험 id 추출
        examId = getIntent().getStringExtra(FirestorePaths.EXTRA_EXAM_ID);

        // firebase에서 시험 id로 시험 이름을 가져와 제목으로 띄움
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.EXAMS)
                .document(examId)
                .get()
                .addOnSuccessListener(doc -> {
                    exam = doc.toObject(Exam.class);
                    binding.examInputExamName.setText(exam.getName());
                    binding.examInputExamDetail.setText(exam.getType() + " · " + exam.getDate());
                    updateSubjectChips();
                });

        // 취소 버튼 (돌아가기)
        binding.examInputCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { finish();}
        });

        // 저장 버튼: 입력한 과목 전부 저장
        binding.examInputSave.setOnClickListener(v -> saveAll());

        // 선택 과목 변경 버튼
        binding.examInputElectiveToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.examInputElectivePanel.getVisibility() == View.VISIBLE) {
                    binding.examInputElectivePanel.setVisibility(View.GONE);
                    binding.examInputElectiveToggle.setText("변경");
                } else {
                    binding.examInputElectivePanel.setVisibility(View.VISIBLE);
                    binding.examInputElectiveToggle.setText("접기");
                }
            }
        });

        // 국어: 누른 것만 체크
        binding.examInputKoreanEonmae.setOnClickListener(v -> {
            binding.examInputKoreanHwajak.setChecked(false);
            binding.examInputKoreanEonmae.setChecked(true);
            korean = "언어와매체";
            updateSummary();
        });
        binding.examInputKoreanHwajak.setOnClickListener(v -> {
            binding.examInputKoreanEonmae.setChecked(false);
            binding.examInputKoreanHwajak.setChecked(true);
            korean = "화법과작문";
            updateSummary();
        });

        // 수학: 누른 것만 체크
        binding.examInputMathHwaktong.setOnClickListener(v -> {
            binding.examInputMathMijeok.setChecked(false);
            binding.examInputMathGiha.setChecked(false);
            binding.examInputMathHwaktong.setChecked(true);
            math = "확률과통계";
            updateSummary();
        });
        binding.examInputMathMijeok.setOnClickListener(v -> {
            binding.examInputMathHwaktong.setChecked(false);
            binding.examInputMathGiha.setChecked(false);
            binding.examInputMathMijeok.setChecked(true);
            math = "미적분";
            updateSummary();
        });
        binding.examInputMathGiha.setOnClickListener(v -> {
            binding.examInputMathHwaktong.setChecked(false);
            binding.examInputMathMijeok.setChecked(false);
            binding.examInputMathGiha.setChecked(true);
            math = "기하";
            updateSummary();
        });

        // 탐구: 과학 한 줄, 사회 한 줄 (RecyclerView)
        binding.examInputScienceRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.examInputScienceRecycler.setAdapter(new ChipAdapter(Subjects.SCIENCE_INQUIRY));
        binding.examInputSocialRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.examInputSocialRecycler.setAdapter(new ChipAdapter(Subjects.SOCIAL_INQUIRY));

        // 틀린 문항: 한 줄에 7칸씩
        binding.examInputWrongRecycler.setLayoutManager(new GridLayoutManager(this, 7));
        updateSummary();

        // 기본값 불러오기
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    statsAgreed = user.isStatsAgreed();

                    // 국어
                    if ("언어와매체".equals(user.getKoreanElective())) binding.examInputKoreanEonmae.performClick();
                    if ("화법과작문".equals(user.getKoreanElective())) binding.examInputKoreanHwajak.performClick();

                    // 수학
                    if ("확률과통계".equals(user.getMathElective())) binding.examInputMathHwaktong.performClick();
                    if ("미적분".equals(user.getMathElective())) binding.examInputMathMijeok.performClick();
                    if ("기하".equals(user.getMathElective())) binding.examInputMathGiha.performClick();

                    // 탐구
                    if (user.getInquiry1() != null) inquiry.add(user.getInquiry1());
                    if (user.getInquiry2() != null) inquiry.add(user.getInquiry2());
                    binding.examInputScienceRecycler.setAdapter(new ChipAdapter(Subjects.SCIENCE_INQUIRY));
                    binding.examInputSocialRecycler.setAdapter(new ChipAdapter(Subjects.SOCIAL_INQUIRY));

                    updateSummary();

                    // 기본값을 적용한 뒤, 이 시험에서 저장한 선택과목과 성적 불러오기
                    loadSelection(uid);
                    loadSaved(uid);
                });
        // 지금 고른 선택과목을 기본값으로 저장
        binding.examInputSaveDefault.setOnClickListener(v -> {
            if (korean.equals("-") || math.equals("-") || inquiry.size() < 2) {
                Toast.makeText(this, "국어, 수학, 탐구 2과목을 모두 골라 주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseFirestore.getInstance()
                    .collection(FirestorePaths.USERS)
                    .document(FirebaseAuth.getInstance().getUid())
                    .update("koreanElective", korean,
                            "mathElective", math,
                            "inquiry1", inquiry.get(0),
                            "inquiry2", inquiry.get(1));
            Toast.makeText(this, "기본값으로 저장했어요", Toast.LENGTH_SHORT).show();
        });

    }

    class ChipViewHolder extends RecyclerView.ViewHolder {
        ItemExamInputChipBinding chipBinding;
        public ChipViewHolder(@NonNull ItemExamInputChipBinding chipBinding) {
            super(chipBinding.getRoot());
            this.chipBinding = chipBinding;
        }

        // 선택과목 하나의 칩에 과목 이름 집어 넣기
        private void bind(String s) {
            chipBinding.chip.setText(s);
            chipBinding.chip.setChecked(inquiry.contains(s));
            chipBinding.chip.setOnClickListener(v -> {
                if (inquiry.contains(s)) {
                    inquiry.remove(s);
                } else if (inquiry.size() < 2) {
                    inquiry.add(s);
                }
                chipBinding.chip.setChecked(inquiry.contains(s));
                updateSummary();
            });
        }
    }

    class ChipAdapter extends  RecyclerView.Adapter<ChipViewHolder> {
        String[] array;

        public ChipAdapter(String[] array) {
            this.array = array;
        }
        @NonNull
        @Override
        public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemExamInputChipBinding chipBinding = ItemExamInputChipBinding.inflate(getLayoutInflater(), parent, false);
            return new ChipViewHolder(chipBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
            holder.bind(array[position]);
        }

        @Override
        public int getItemCount() {
            return array.length;
        }
    }

    // 선택 과목 요약 칸 텍스트 변경
    void updateSummary() {
        String inquiryText = "-";
        if (inquiry.size() == 1) inquiryText = inquiry.get(0);
        if (inquiry.size() == 2) inquiryText = inquiry.get(0) + ", " + inquiry.get(1);
        binding.examInputElectiveSummary.setText(korean + " · " + math + " · " + inquiryText);
        updateSubjectChips();
    }

    // 누른 과목 칩만 체크하고 고른 과목을 subject에 저장
    void selectSubject(CheckBox chip) {
        // 바꾸기 전 과목의 입력 내용 보관
        if (subject != null) {
            rawDraft.put(subject, binding.examInputRawScore.getText().toString());
            gradeDraft.put(subject, binding.examInputGrade.getText().toString());
            wrongDraft.put(subject, new ArrayList<>(wrongNumbers));
        }

        for (CheckBox c : subjectChips) {
            c.setChecked(false);
        }
        chip.setChecked(true);
        subject = chip.getText().toString();

        // 임시 보관한 내용 → 저장된 성적 → 빈칸 순서로 채우기
        wrongNumbers.clear();
        if (rawDraft.containsKey(subject)) {
            binding.examInputRawScore.setText(rawDraft.get(subject));
            binding.examInputGrade.setText(gradeDraft.get(subject));
            wrongNumbers.addAll(wrongDraft.get(subject));
        } else if (saved.containsKey(subject)) {
            Score score = saved.get(subject);
            binding.examInputRawScore.setText(String.valueOf(score.getRawScore()));
            binding.examInputGrade.setText("");
            if (score.getGrade() != null) binding.examInputGrade.setText(String.valueOf(score.getGrade()));
            wrongNumbers.addAll(score.getWrongNumbers());
        } else {
            binding.examInputRawScore.setText("");
            binding.examInputGrade.setText("");
        }

        // 원점수 칸 안내 글자를 과목 만점에 맞춤
        if (subject.equals("국어") || subject.equals("수학") || subject.equals("영어")) {
            binding.examInputRawScore.setHint("0~100");
        } else {
            binding.examInputRawScore.setHint("0~50");
        }

        // 번호 칸 다시 그리기
        showWrongNumbers();
    }

    // 탐구 칩에 고른 탐구 이름을 넣고, 이 시험에 없는 과목 칩은 숨김
    void updateSubjectChips() {
        if (exam == null) return;

        binding.subjectInquiry1.setText("");
        binding.subjectInquiry2.setText("");
        if (inquiry.size() >= 1) binding.subjectInquiry1.setText(inquiry.get(0));
        if (inquiry.size() >= 2) binding.subjectInquiry2.setText(inquiry.get(1));

        for (CheckBox c : subjectChips) {
            if (exam.getSubjects().contains(c.getText().toString())) {
                c.setVisibility(View.VISIBLE);
            } else {
                c.setVisibility(View.GONE);
            }
        }
    }

    //
    // 고른 과목의 문항 수만큼 번호 칸을 만들고, 선택 개수와 선택과목 범위 안내 표시
    void showWrongNumbers() {
        int count = exam.getQuestionCounts().get(subject);
        binding.examInputWrongRecycler.setAdapter(new NumberAdapter(count));
        binding.examInputWrongCount.setText(wrongNumbers.size() + "개 선택됨");

        if (subject.equals("국어")) {
            binding.examInputElectiveHint.setText("35~45번: " + korean);
            binding.examInputElectiveHint.setVisibility(View.VISIBLE);
        } else if (subject.equals("수학")) {
            binding.examInputElectiveHint.setText("23~30번: " + math);
            binding.examInputElectiveHint.setVisibility(View.VISIBLE);
        } else {
            binding.examInputElectiveHint.setVisibility(View.GONE);
        }
    }

    class NumberViewHolder extends RecyclerView.ViewHolder {
        ItemWrongNumberBinding numberBinding;

        public NumberViewHolder(@NonNull ItemWrongNumberBinding numberBinding) {
            super(numberBinding.getRoot());
            this.numberBinding = numberBinding;
        }

        private void bind(int number) {
            numberBinding.wrongNumber.setText(String.valueOf(number));
            numberBinding.wrongNumber.setSelected(wrongNumbers.contains(number));
            numberBinding.wrongNumber.setOnClickListener(v -> {
                if (wrongNumbers.contains(number)) {
                    wrongNumbers.remove(Integer.valueOf(number));
                } else {
                    wrongNumbers.add(number);
                }
                numberBinding.wrongNumber.setSelected(wrongNumbers.contains(number));
                binding.examInputWrongCount.setText(wrongNumbers.size() + "개 선택됨");
            });
        }
    }

    class NumberAdapter extends RecyclerView.Adapter<NumberViewHolder> {
        int count;

        public NumberAdapter(int count) {
            this.count = count;
        }

        @NonNull
        @Override
        public NumberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemWrongNumberBinding numberBinding = ItemWrongNumberBinding.inflate(getLayoutInflater(), parent, false);
            return new NumberViewHolder(numberBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull NumberViewHolder holder, int position) {
            holder.bind(position + 1);
        }

        @Override
        public int getItemCount() {
            return count;
        }
    }

    // 이 시험에 저장된 내 성적을 과목별로 불러오기
    void loadSaved(String uid) {
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.SCORES)
                .whereEqualTo("uid", uid)
                .whereEqualTo("examId", examId)
                .get()
                .addOnSuccessListener(result -> {
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        Score score = doc.toObject(Score.class);
                        saved.put(score.getSubject(), score);
                    }
                });
    }

    // 이 시험에서 저장한 선택과목이 있으면 기본값 대신 그걸로 체크
    void loadSelection(String uid) {
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.SELECTIONS)
                .document(uid + "_" + examId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String k = doc.getString("korean");
                    if ("언어와매체".equals(k)) binding.examInputKoreanEonmae.performClick();
                    if ("화법과작문".equals(k)) binding.examInputKoreanHwajak.performClick();

                    String m = doc.getString("math");
                    if ("확률과통계".equals(m)) binding.examInputMathHwaktong.performClick();
                    if ("미적분".equals(m)) binding.examInputMathMijeok.performClick();
                    if ("기하".equals(m)) binding.examInputMathGiha.performClick();

                    inquiry.clear();
                    if (doc.getString("inquiry1") != null) inquiry.add(doc.getString("inquiry1"));
                    if (doc.getString("inquiry2") != null) inquiry.add(doc.getString("inquiry2"));
                    binding.examInputScienceRecycler.setAdapter(new ChipAdapter(Subjects.SCIENCE_INQUIRY));
                    binding.examInputSocialRecycler.setAdapter(new ChipAdapter(Subjects.SOCIAL_INQUIRY));
                    updateSummary();
                });
    }

    // 원점수를 입력한 과목을 전부 검사한 뒤, 선택과목과 과목별 성적을 저장
    void saveAll() {
        // 지금 보고 있는 과목의 입력도 보관함에 넣기
        if (subject != null) {
            rawDraft.put(subject, binding.examInputRawScore.getText().toString());
            gradeDraft.put(subject, binding.examInputGrade.getText().toString());
            wrongDraft.put(subject, new ArrayList<>(wrongNumbers));
        }

        // 1) 검사: 하나라도 문제가 있으면 아무것도 저장하지 않음
        int count = 0;
        for (String name : rawDraft.keySet()) {
            String rawText = rawDraft.get(name);
            if (rawText.isEmpty()) continue;

            // 국어·수학·영어는 100점, 한국사·탐구는 50점 만점
            int max = 50;
            if (name.equals("국어") || name.equals("수학") || name.equals("영어")) max = 100;
            if (Integer.parseInt(rawText) > max) {
                Toast.makeText(this, name + " 원점수는 0~" + max + " 사이로 입력해 주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String gradeText = gradeDraft.get(name);
            if (!gradeText.isEmpty() && (Integer.parseInt(gradeText) < 1 || Integer.parseInt(gradeText) > 9)) {
                Toast.makeText(this, name + " 등급은 1~9 사이로 입력해 주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            if ((name.equals("국어") && korean.equals("-")) || (name.equals("수학") && math.equals("-"))) {
                Toast.makeText(this, name + " 선택과목을 골라 주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            count++;
        }
        if (count == 0) {
            Toast.makeText(this, "원점수를 입력한 과목이 없어요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2) 저장
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 이 시험에서 고른 선택과목 (시험마다 문서 하나, 덮어씀)
        Map<String, Object> selection = new HashMap<>();
        selection.put("korean", korean);
        selection.put("math", math);
        if (inquiry.size() >= 1) selection.put("inquiry1", inquiry.get(0));
        if (inquiry.size() >= 2) selection.put("inquiry2", inquiry.get(1));
        db.collection(FirestorePaths.SELECTIONS).document(uid + "_" + examId).set(selection);

        // 원점수를 입력한 과목만 scores에 저장 (같은 시험·과목이면 덮어씀)
        for (String name : rawDraft.keySet()) {
            String rawText = rawDraft.get(name);
            if (rawText.isEmpty()) continue;

            String elective = null;
            if (name.equals("국어")) elective = korean;
            if (name.equals("수학")) elective = math;

            Integer grade = null;
            if (!gradeDraft.get(name).isEmpty()) grade = Integer.parseInt(gradeDraft.get(name));

            List<Integer> wrong = new ArrayList<>(wrongDraft.get(name));
            Collections.sort(wrong);

            Score score = new Score();
            score.setUid(uid);
            score.setExamId(examId);
            score.setSubject(name);
            score.setElective(elective);
            score.setRawScore(Integer.parseInt(rawText));
            score.setGrade(grade);
            score.setWrongNumbers(wrong);
            score.setStatsIncluded(statsAgreed);
            db.collection(FirestorePaths.SCORES).document(Score.makeId(uid, examId, name)).set(score);
        }

        // 시험 고르기·성적 입력을 닫고 기록 탭으로 돌아가기
        Toast.makeText(this, count + "과목 저장했어요", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
