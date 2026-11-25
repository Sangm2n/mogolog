package com.team.mogolog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.team.mogolog.databinding.ActivityExamPickBinding;
import com.team.mogolog.databinding.ItemExamCardBinding;
import com.team.mogolog.model.Exam;
import com.team.mogolog.model.FirestorePaths;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExamPickActivity extends AppCompatActivity {

    private ActivityExamPickBinding binding;
    private final List<Exam> list = new ArrayList<>();
    private String selectedType = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        //setContentView(R.layout.activity_exam_pick);
        binding = ActivityExamPickBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 취소 버튼
        binding.examPickCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 데이터 준비, RecyclerView에 LayoutManager, ExamAdapter 등록
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.EXAMS)
                .orderBy("date", Query.Direction.DESCENDING)      // 날짜 최신순
                .get()
                .addOnSuccessListener(result -> {
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        Exam exam = doc.toObject(Exam.class);     // 문서 → Exam 객체
                        if (exam != null && exam.getDate() != null && exam.getDate().compareTo(today) <= 0) {
                            list.add(exam);
                        }
                    }
                    binding.examPickRecycler.setLayoutManager(new LinearLayoutManager(this));
                    showExams(); // setAdapter 역할
                })
                .addOnFailureListener(e -> {
                    binding.examPickRecycler.setVisibility(View.GONE);
                    binding.examPickEmpty.setText("시험 목록을 불러오지 못했어요.");
                    binding.examPickEmpty.setVisibility(View.VISIBLE);
                });
        // 필터 버튼에 리스너 등록
        binding.examPickFilterAll.setOnClickListener(v -> selectFilter(v, null));
        binding.examPickFilterKice.setOnClickListener(v -> selectFilter(v, "평가원"));
        binding.examPickFilterEdu.setOnClickListener(v -> selectFilter(v, "교육청"));
        binding.examPickFilterPrivate.setOnClickListener(v -> selectFilter(v, "사설"));
        binding.examPickFilterAll.setSelected(true);   // 처음엔 "전체" 선택

        // 검색 버튼에 리스너 등록
        binding.examPickSearchButton.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(binding.examPickSearchInput.getWindowToken(), 0);
            binding.examPickSearchInput.clearFocus();
            showExams();
        });
    }

    class ExamViewHolder extends RecyclerView.ViewHolder {

        ItemExamCardBinding cardBinding;
        public ExamViewHolder(@NonNull ItemExamCardBinding cardBinding) {
            super(cardBinding.getRoot());
            this.cardBinding = cardBinding;
        }

        public void bind(Exam exam) {
            cardBinding.examCardName.setText(exam.getName());

            cardBinding.examCardType.setText(exam.getType());
            if ("교육청".equals(exam.getType())) {
                cardBinding.examCardType.setBackgroundResource(R.drawable.bg_badge_education);
                cardBinding.examCardType.setTextColor(
                        ContextCompat.getColor(ExamPickActivity.this, R.color.badge_education_text));
            } else if ("사설".equals(exam.getType())) {
                cardBinding.examCardType.setBackgroundResource(R.drawable.bg_badge_private);
                cardBinding.examCardType.setTextColor(
                        ContextCompat.getColor(ExamPickActivity.this, R.color.badge_private_text));
            } else {
                cardBinding.examCardType.setBackgroundResource(R.drawable.bg_badge_kice);
                cardBinding.examCardType.setTextColor(
                        ContextCompat.getColor(ExamPickActivity.this, R.color.badge_kice_text));
            }

            cardBinding.examCardDetail.setText(exam.getDate());

            // 시험 배너를 누르면 해당 시험에 대한 정보를 등록하는 Activity로 넘어감
            itemView.setOnClickListener(v -> {
                startActivity(new Intent(ExamPickActivity.this, ExamInputActivity.class)
                        .putExtra(FirestorePaths.EXTRA_EXAM_ID, exam.getId()));

            });
        }
    }

    class ExamAdapter extends RecyclerView.Adapter<ExamViewHolder> {

        List<Exam> list;
        
        public ExamAdapter(List<Exam> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemExamCardBinding cardBinding = ItemExamCardBinding.inflate(getLayoutInflater(), parent, false);
            return new ExamViewHolder(cardBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
            Exam exam = list.get(position);
            holder.bind(exam);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    // 검색에서 목록을 거르는 함수
    private void showExams() {
        String word = binding.examPickSearchInput.getText().toString().replace(" ", "");

        List<Exam> result = new ArrayList<>();
        for (Exam exam : list) {
            boolean typeOk = (selectedType == null) || selectedType.equals(exam.getType());
            boolean wordOk = exam.getName().replace(" ", "").contains(word);
            if (typeOk && wordOk) {
                result.add(exam);
            }
        }

        if (result.isEmpty()) {
            binding.examPickRecycler.setVisibility(View.GONE);
            binding.examPickEmpty.setText("해당하는 시험이 없어요.");
            binding.examPickEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.examPickEmpty.setVisibility(View.GONE);
            binding.examPickRecycler.setVisibility(View.VISIBLE);
        }
        binding.examPickRecycler.setAdapter(new ExamAdapter(result));
    }

    // 필터 버튼
    private void selectFilter(View button, String type) {
        binding.examPickFilterAll.setSelected(false);
        binding.examPickFilterKice.setSelected(false);
        binding.examPickFilterEdu.setSelected(false);
        binding.examPickFilterPrivate.setSelected(false);
        button.setSelected(true);

        selectedType = type;
        showExams();
    }
}