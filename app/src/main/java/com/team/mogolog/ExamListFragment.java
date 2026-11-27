package com.team.mogolog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.team.mogolog.databinding.FragmentExamListBinding;
import com.team.mogolog.databinding.ItemExamCardBinding;
import com.team.mogolog.model.Exam;
import com.team.mogolog.model.FirestorePaths;
import com.team.mogolog.model.Score;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 시험 기록 탭 (A5): 내가 성적을 넣은 시험 목록
public class ExamListFragment extends Fragment {

    private FragmentExamListBinding binding;
    private List<Exam> myExams = new ArrayList<>(); // 성적을 넣은 시험
    private Map<String, String> scoreText = new HashMap<>(); // 시험별 "국어 92 · 수학 84"
    private String selectedType = null; // null이면 전체

    public ExamListFragment() {
        super(R.layout.fragment_exam_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentExamListBinding.bind(view);

        // + 버튼 → 시험 고르기
        binding.examListAdd.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), ExamPickActivity.class)));

        binding.examListRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 필터 버튼
        binding.examListFilterAll.setOnClickListener(v -> selectFilter(v, null));
        binding.examListFilterKice.setOnClickListener(v -> selectFilter(v, "평가원"));
        binding.examListFilterEdu.setOnClickListener(v -> selectFilter(v, "교육청"));
        binding.examListFilterPrivate.setOnClickListener(v -> selectFilter(v, "사설"));
        binding.examListFilterAll.setSelected(true);
    }

    // 탭이 보일 때마다 새로 불러오기 (성적 저장 후 돌아왔을 때 반영)
    @Override
    public void onResume() {
        super.onResume();
        loadMyScores();
    }

    // 내 성적을 불러와 시험별 점수 글자 만들기
    private void loadMyScores() {
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.SCORES)
                .whereEqualTo("uid", FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(result -> {
                    scoreText.clear();
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        Score score = doc.toObject(Score.class);
                        String examId = score.getExamId();
                        String text = score.getSubject() + " " + score.getRawScore();
                        if (scoreText.get(examId) == null) {
                            scoreText.put(examId, text);
                        } else {
                            scoreText.put(examId, scoreText.get(examId) + " · " + text);
                        }
                    }
                    loadExams();
                });
    }

    // 시험 목록을 최신순으로 불러와 성적 있는 시험만 남기기
    private void loadExams() {
        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.EXAMS)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(result -> {
                    myExams.clear();
                    for (DocumentSnapshot doc : result.getDocuments()) {
                        Exam exam = doc.toObject(Exam.class);
                        if (scoreText.containsKey(exam.getId())) {
                            myExams.add(exam);
                        }
                    }
                    showExams();
                });
    }

    // 고른 종류만 보여주기
    private void showExams() {
        List<Exam> result = new ArrayList<>();
        for (Exam exam : myExams) {
            if (selectedType == null || selectedType.equals(exam.getType())) {
                result.add(exam);
            }
        }

        if (result.isEmpty()) {
            binding.examListRecycler.setVisibility(View.GONE);
            binding.examListEmpty.setVisibility(View.VISIBLE);
            if (!myExams.isEmpty()) {
                binding.examListEmpty.setText("해당하는 시험이 없어요.");
            }
        } else {
            binding.examListEmpty.setVisibility(View.GONE);
            binding.examListRecycler.setVisibility(View.VISIBLE);
        }
        binding.examListRecycler.setAdapter(new ExamAdapter(result));
    }

    // 필터 버튼
    private void selectFilter(View button, String type) {
        binding.examListFilterAll.setSelected(false);
        binding.examListFilterKice.setSelected(false);
        binding.examListFilterEdu.setSelected(false);
        binding.examListFilterPrivate.setSelected(false);
        button.setSelected(true);

        selectedType = type;
        showExams();
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
                cardBinding.examCardType.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_education_text));
            } else if ("사설".equals(exam.getType())) {
                cardBinding.examCardType.setBackgroundResource(R.drawable.bg_badge_private);
                cardBinding.examCardType.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_private_text));
            } else {
                cardBinding.examCardType.setBackgroundResource(R.drawable.bg_badge_kice);
                cardBinding.examCardType.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_kice_text));
            }

            // 날짜 대신 과목별 점수
            cardBinding.examCardDetail.setText(scoreText.get(exam.getId()));

            // 카드 누르기 → 지금은 성적 입력 (시험 상세를 만들면 바꾸기)
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ExamInputActivity.class);
                intent.putExtra(FirestorePaths.EXTRA_EXAM_ID, exam.getId());
                startActivity(intent);
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
            holder.bind(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
