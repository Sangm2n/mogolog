package com.team.mogolog;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.mogolog.model.Exam;
import com.team.mogolog.model.FirestorePaths;
import com.team.mogolog.model.Subjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommunityFragment extends Fragment {

    private CommunityPostsController postsController;

    private Spinner spinnerExam;
    private Spinner spinnerSubject;
    private Spinner spinnerElective;
    private Spinner spinnerQuestion;

    private View layoutElective;
    private TextView textSelection;
    private TextView textStatus;
    private ProgressBar progress;
    private Button buttonReload;

    private final List<Exam> exams = new ArrayList<>();
    private final List<String> subjects = new ArrayList<>();

    // null은 공통 문항을 뜻한다.
    private final List<String> electives = new ArrayList<>();

    // 0은 전체 문항을 뜻한다.
    private final List<Integer> questionNumbers = new ArrayList<>();

    private Exam selectedExam;
    private String selectedSubject;
    private String selectedElective;
    private int selectedQuestionNumber;

    private boolean loading = false;
    private long loadVersion = 0;

    public CommunityFragment() {
        super(R.layout.fragment_community);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        spinnerExam = view.findViewById(R.id.spinnerCommunityExam);
        spinnerSubject = view.findViewById(R.id.spinnerCommunitySubject);
        spinnerElective = view.findViewById(R.id.spinnerCommunityElective);
        spinnerQuestion = view.findViewById(R.id.spinnerCommunityQuestion);

        layoutElective =
                view.findViewById(R.id.layoutCommunityElective);

        textSelection =
                view.findViewById(R.id.textCommunitySelection);

        textStatus =
                view.findViewById(R.id.textCommunityStatus);

        progress =
                view.findViewById(R.id.progressCommunity);

        buttonReload =
                view.findViewById(R.id.buttonCommunityReload);

        postsController = new CommunityPostsController(view);

        setSelectionListener(spinnerExam, position -> {
            if (loading || position >= exams.size()) {
                return;
            }

            Exam exam = exams.get(position);

            if (selectedExam == exam) {
                return;
            }

            selectExam(exam);
        });

        setSelectionListener(spinnerSubject, position -> {
            if (loading || position >= subjects.size()) {
                return;
            }

            String subject = subjects.get(position);

            if (subject.equals(selectedSubject)) {
                return;
            }

            selectedSubject = subject;
            rebuildElectives();
        });

        setSelectionListener(spinnerElective, position -> {
            if (loading || position >= electives.size()) {
                return;
            }

            String elective = electives.get(position);

            if (sameValue(elective, selectedElective)) {
                return;
            }

            selectedElective = elective;
            rebuildQuestions();
        });

        setSelectionListener(spinnerQuestion, position -> {
            if (loading || position >= questionNumbers.size()) {
                return;
            }

            selectedQuestionNumber = questionNumbers.get(position);
            showSelection();
        });

        buttonReload.setOnClickListener(v -> loadExams());

        loadExams();
    }

    private void loadExams() {
        final View requestView = getView();

        if (requestView == null) {
            return;
        }

        final long requestVersion = ++loadVersion;

        postsController.clear();
        loading = true;

        selectedExam = null;
        selectedSubject = null;
        selectedElective = null;
        selectedQuestionNumber = 0;

        exams.clear();
        subjects.clear();
        electives.clear();
        questionNumbers.clear();

        setItems(spinnerExam, new ArrayList<>());
        setItems(spinnerSubject, new ArrayList<>());
        setItems(spinnerElective, new ArrayList<>());
        setItems(spinnerQuestion, new ArrayList<>());

        layoutElective.setVisibility(View.GONE);
        setSelectorsEnabled(false);

        buttonReload.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        textSelection.setText("시험을 불러오는 중입니다.");
        textStatus.setText("시험 목록을 불러오고 있습니다.");

        FirebaseFirestore.getInstance()
                .collection(FirestorePaths.EXAMS)
                .get()
                .addOnCompleteListener(task -> {
                    // 화면이 닫혔거나 새 요청이 시작됐으면 결과를 무시한다.
                    if (getView() != requestView
                            || requestVersion != loadVersion) {
                        return;
                    }

                    loading = false;
                    progress.setVisibility(View.GONE);
                    buttonReload.setEnabled(true);

                    if (!task.isSuccessful()) {
                        textSelection.setText(
                                "시험 목록을 불러오지 못했습니다."
                        );

                        Exception exception = task.getException();

                        String detail = exception == null
                                ? "알 수 없는 오류"
                                : exception.getLocalizedMessage();

                        textStatus.setText(
                                "다시 불러오기를 눌러주세요.\n" + detail
                        );
                        return;
                    }

                    try {
                        for (DocumentSnapshot document
                                : task.getResult().getDocuments()) {

                            Exam exam = document.toObject(Exam.class);

                            if (exam != null) {
                                exam.setId(document.getId());
                                exams.add(exam);
                            }
                        }
                    } catch (RuntimeException exception) {
                        exams.clear();
                        textSelection.setText(
                                "시험 데이터 형식을 확인해주세요."
                        );
                        textStatus.setText(
                                exception.getLocalizedMessage()
                        );
                        return;
                    }

                    // 날짜가 최신인 시험부터 표시한다.
                    exams.sort((first, second) -> {
                        int dateOrder = safe(second.getDate())
                                .compareTo(safe(first.getDate()));

                        if (dateOrder != 0) {
                            return dateOrder;
                        }

                        return safe(first.getId())
                                .compareTo(safe(second.getId()));
                    });

                    if (exams.isEmpty()) {
                        textSelection.setText("등록된 시험이 없습니다.");
                        textStatus.setText(
                                "Firestore의 exams에 시험을 등록해주세요."
                        );
                        return;
                    }

                    List<String> labels = new ArrayList<>();

                    for (Exam exam : exams) {
                        String name = exam.getName();

                        if (name == null || name.trim().isEmpty()) {
                            name = exam.getId();
                        }

                        String date = safe(exam.getDate());

                        labels.add(
                                date.isEmpty()
                                        ? name
                                        : name + " (" + date + ")"
                        );
                    }

                    setItems(spinnerExam, labels);
                    spinnerExam.setEnabled(true);

                    selectExam(exams.get(0));
                });
    }

    private void selectExam(Exam exam) {
        selectedExam = exam;
        selectedSubject = null;
        selectedElective = null;
        selectedQuestionNumber = 0;

        subjects.clear();

        if (exam.getSubjects() != null) {
            for (String subject : exam.getSubjects()) {
                if (subject != null
                        && !subject.trim().isEmpty()
                        && !subjects.contains(subject)) {
                    subjects.add(subject);
                }
            }
        }

        setItems(spinnerSubject, subjects);
        spinnerSubject.setEnabled(!subjects.isEmpty());

        if (subjects.isEmpty()) {
            postsController.clear();
            electives.clear();
            questionNumbers.clear();

            setItems(spinnerElective, new ArrayList<>());
            setItems(spinnerQuestion, new ArrayList<>());

            spinnerElective.setEnabled(false);
            spinnerQuestion.setEnabled(false);
            layoutElective.setVisibility(View.GONE);

            textSelection.setText("이 시험에는 과목 정보가 없습니다.");
            textStatus.setText(
                    "시험 데이터의 subjects를 확인해주세요."
            );
            return;
        }

        selectedSubject = subjects.get(0);
        rebuildElectives();
    }

    private void rebuildElectives() {
        electives.clear();

        List<String> labels = new ArrayList<>();

        electives.add(null);

        boolean korean = Subjects.COMMON[0].equals(selectedSubject);
        boolean math = Subjects.COMMON[1].equals(selectedSubject);

        if (korean || math) {
            labels.add("공통 문항");

            String[] values = korean
                    ? Subjects.KOREAN_ELECTIVES
                    : Subjects.MATH_ELECTIVES;

            for (String elective : values) {
                electives.add(elective);
                labels.add(elective);
            }

            layoutElective.setVisibility(View.VISIBLE);
            spinnerElective.setEnabled(true);
        } else {
            labels.add("공통");
            layoutElective.setVisibility(View.GONE);
            spinnerElective.setEnabled(false);
        }

        selectedElective = null;

        setItems(spinnerElective, labels);
        rebuildQuestions();
    }

    private void rebuildQuestions() {
        questionNumbers.clear();

        List<String> labels = new ArrayList<>();

        questionNumbers.add(0);
        labels.add("전체 문항");

        int count = getQuestionCount();
        int start = 1;
        int end = count;

        /*
         * 현재 기획의 개편 전 수능 기준:
         * 국어 공통 1~34, 선택 35~45
         * 수학 공통 1~22, 선택 23~30
         */
        if (Subjects.COMMON[0].equals(selectedSubject)) {
            if (selectedElective == null) {
                end = Math.min(count, 34);
            } else {
                start = 35;
            }
        } else if (Subjects.COMMON[1].equals(selectedSubject)) {
            if (selectedElective == null) {
                end = Math.min(count, 22);
            } else {
                start = 23;
            }
        }

        for (int number = start; number <= end; number++) {
            questionNumbers.add(number);
            labels.add(number + "번");
        }

        selectedQuestionNumber = 0;

        setItems(spinnerQuestion, labels);
        spinnerQuestion.setEnabled(end >= start && count > 0);

        showSelection();
    }

    private int getQuestionCount() {
        if (selectedExam == null || selectedSubject == null) {
            return 0;
        }

        Map<String, Integer> counts =
                selectedExam.getQuestionCounts();

        if (counts == null) {
            return 0;
        }

        Integer count = counts.get(selectedSubject);

        return count == null ? 0 : Math.max(0, count);
    }

    private void showSelection() {
        if (selectedExam == null || selectedSubject == null) {
            return;
        }

        String examName = selectedExam.getName();

        if (examName == null || examName.trim().isEmpty()) {
            examName = selectedExam.getId();
        }

        StringBuilder summary = new StringBuilder();

        summary.append(examName);
        summary.append("\n");
        summary.append(selectedSubject);

        boolean hasElective =
                Subjects.COMMON[0].equals(selectedSubject)
                        || Subjects.COMMON[1].equals(selectedSubject);

        if (hasElective) {
            summary.append(" · ");
            summary.append(
                    selectedElective == null
                            ? "공통 문항"
                            : selectedElective
            );
        }

        summary.append(" · ");
        summary.append(
                selectedQuestionNumber == 0
                        ? "전체 문항"
                        : selectedQuestionNumber + "번"
        );

        textSelection.setText(summary.toString());

        if (getQuestionCount() <= 0) {
            postsController.clear();
            textStatus.setText(
                    "이 과목의 문항 수 정보가 없습니다.\n"
                            + "시험 데이터의 questionCounts를 확인해주세요."
            );
        } else {
            postsController.select(
                    selectedExam.getId(), examName, selectedSubject,
                    selectedElective, selectedQuestionNumber, questionNumbers
            );
        }
    }

    private void setSelectorsEnabled(boolean enabled) {
        spinnerExam.setEnabled(enabled);
        spinnerSubject.setEnabled(enabled);
        spinnerElective.setEnabled(enabled);
        spinnerQuestion.setEnabled(enabled);
    }

    private void setItems(Spinner spinner, List<String> labels) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>(labels)
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private void setSelectionListener(
            Spinner spinner,
            SelectionAction action) {

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (getView() != null && position >= 0) {
                            action.onSelected(position);
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );
    }

    private boolean sameValue(String first, String second) {
        return first == null
                ? second == null
                : first.equals(second);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private interface SelectionAction {
        void onSelected(int position);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (postsController != null) postsController.setActive(!isHidden());
    }

    @Override
    public void onStop() {
        if (postsController != null) postsController.setActive(false);
        super.onStop();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (postsController != null) postsController.setActive(!hidden && isResumed());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (postsController != null) postsController.setActive(!isHidden());
    }

    @Override
    public void onDestroyView() {
        if (postsController != null) {
            postsController.destroy();
            postsController = null;
        }
        loadVersion++;

        spinnerExam.setOnItemSelectedListener(null);
        spinnerSubject.setOnItemSelectedListener(null);
        spinnerElective.setOnItemSelectedListener(null);
        spinnerQuestion.setOnItemSelectedListener(null);

        super.onDestroyView();
    }
}