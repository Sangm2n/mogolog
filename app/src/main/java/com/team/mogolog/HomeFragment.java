package com.team.mogolog;

import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.team.mogolog.model.Exam;
import com.team.mogolog.model.Score;
import com.team.mogolog.model.Subjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
      view.findViewById(R.id.buttonLogout)
        .setOnClickListener(button -> {
            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(
                    requireContext(),
                    LoginActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            requireActivity().finish();
        });
        HomeDataModel model = new ViewModelProvider(this).get(HomeDataModel.class);
        RadioGroup subjects = view.findViewById(R.id.home_trend_subjects);
        subjects.setOnCheckedChangeListener((group, id) -> showTrend(view, model.current(), id));
        view.findViewById(R.id.home_review_action).setOnClickListener(button -> {
            if (!getChildFragmentManager().isStateSaved()
                    && getChildFragmentManager().findFragmentByTag(HomeReviewDialog.TAG) == null) {
                new HomeReviewDialog().showNow(getChildFragmentManager(), HomeReviewDialog.TAG);
            }
        });
        view.findViewById(R.id.home_data_retry).setOnClickListener(button -> model.reload());
        view.findViewById(R.id.home_demo_action).setOnClickListener(button -> model.toggleDemo());
        view.findViewById(R.id.home_notifications).setEnabled(false);

        model.data().observe(getViewLifecycleOwner(), data -> {
            showNextExam(view, data);
            showRecentExam(view, data);
            showReviewProgress(view, data);
            showTrend(view, data, subjects.getCheckedRadioButtonId());
            showDataStatus(view, model, data);
        });
    }

    private void showNextExam(View view, HomeData data) {
        long today = HomeData.today();
        Exam exam = data.nextExam(today);
        TextView badge = view.findViewById(R.id.home_next_exam_badge);
        TextView dayCount = view.findViewById(R.id.home_next_exam_day_count);
        TextView name = view.findViewById(R.id.home_next_exam_name);
        if (exam == null) {
            int message = data.loading ? R.string.value_placeholder
                    : data.examsAvailable ? R.string.home_no_next_exam : R.string.home_exam_load_issue;
            name.setText(message);
            badge.setVisibility(View.INVISIBLE);
            dayCount.setText(R.string.value_placeholder);
            return;
        }
        name.setText(exam.getName() == null ? exam.getId() : exam.getName());
        badge.setText(exam.getType());
        badge.setVisibility(exam.getType() == null ? View.INVISIBLE : View.VISIBLE);
        long days = HomeData.dayNumber(exam.getDate()) - today;
        dayCount.setText(days == 0 ? getString(R.string.home_exam_dday_today)
                : getString(R.string.home_exam_days, days));
    }

    private void showRecentExam(View view, HomeData data) {
        List<Score> dated = data.datedScores(null);
        TextView name = view.findViewById(R.id.home_recent_exam_name);
        Exam exam = dated.isEmpty() ? null : data.exam(dated.get(dated.size() - 1).getExamId());
        Map<String, Score> recent = new HashMap<>();
        if (exam == null) {
            name.setText(data.scoresAvailable && data.examsAvailable
                    ? R.string.home_no_recent_exam : R.string.value_placeholder);
        } else {
            name.setText(exam.getName() == null ? exam.getId() : exam.getName());
            for (Score score : dated) {
                if (exam.getId().equals(score.getExamId())) {
                    recent.put(score.getSubject(), score);
                }
            }
        }
        setScore(view, R.id.home_korean_score, recent.get(Subjects.COMMON[0]));
        setScore(view, R.id.home_math_score, recent.get(Subjects.COMMON[1]));
        setScore(view, R.id.home_english_score, recent.get(Subjects.COMMON[2]));

        Score inquiry = null;
        if (exam != null && exam.getSubjects() != null) {
            for (String subject : exam.getSubjects()) {
                if (!java.util.Arrays.asList(Subjects.COMMON).contains(subject) && recent.containsKey(subject)) {
                    inquiry = recent.get(subject);
                    break;
                }
            }
        }
        if (inquiry == null) {
            // Older exam documents can omit the subject list.
            for (String subject : Subjects.SOCIAL_INQUIRY) {
                if (recent.containsKey(subject)) {
                    inquiry = recent.get(subject);
                    break;
                }
            }
        }
        if (inquiry == null) {
            for (String subject : Subjects.SCIENCE_INQUIRY) {
                if (recent.containsKey(subject)) {
                    inquiry = recent.get(subject);
                    break;
                }
            }
        }
        TextView inquiryLabel = view.findViewById(R.id.home_inquiry_subject);
        inquiryLabel.setText(inquiry == null ? getString(R.string.subject_inquiry_label) : inquiry.getSubject());
        setScore(view, R.id.home_inquiry_score, inquiry);
    }

    private void setScore(View view, int viewId, Score score) {
        TextView value = view.findViewById(viewId);
        value.setText(score == null ? getString(R.string.value_placeholder)
                : String.valueOf(score.getRawScore()));
    }

    private void showReviewProgress(View view, HomeData data) {
        TextView count = view.findViewById(R.id.home_unreviewed_count);
        TextView percent = view.findViewById(R.id.home_review_progress_value);
        ProgressBar progress = view.findViewById(R.id.home_review_progress);
        if (!data.scoresAvailable || !data.notesAvailable) {
            count.setText(R.string.value_placeholder);
            percent.setText(R.string.value_placeholder);
            progress.setProgress(0);
            return;
        }
        ReviewDrafts drafts = new ReviewDrafts(requireContext(), data.owner);
        List<HomeData.ReviewItem> items = data.reviewItems(drafts::done);
        int pending = 0;
        for (HomeData.ReviewItem item : items) {
            if (!item.done) {
                pending++;
            }
        }
        int completion = items.isEmpty() ? 0
                : Math.round(100f * (items.size() - pending) / items.size());
        count.setText(getString(R.string.home_review_count, pending));
        percent.setText(getString(R.string.home_review_percent, completion));
        progress.setProgress(completion);
    }

    private void showTrend(View view, HomeData data, int selection) {
        String subject = Subjects.COMMON[1];
        if (selection == R.id.home_trend_korean) {
            subject = Subjects.COMMON[0];
        } else if (selection == R.id.home_trend_english) {
            subject = Subjects.COMMON[2];
        }
        List<ScoreTrendView.Point> trend = new ArrayList<>();
        for (Score score : data.datedScores(subject)) {
            Exam exam = data.exam(score.getExamId());
            String label = "사설".equals(exam.getType()) && exam.getProvider() != null
                    ? exam.getProvider()
                    : getString(R.string.home_trend_month, Integer.parseInt(exam.getDate().substring(5, 7)));
            trend.add(new ScoreTrendView.Point(label, score.getRawScore()));
        }
        ScoreTrendView chart = view.findViewById(R.id.home_score_trend_chart);
        chart.setPoints(trend);
    }

    private void showDataStatus(View view, HomeDataModel model, HomeData data) {
        int message = 0;
        boolean failed = !data.loading && (!data.examsAvailable
                || (data.owner != null && (!data.scoresAvailable || !data.notesAvailable)));
        if (model.isDemo()) {
            message = R.string.home_demo_active;
        } else if (data.loading) {
            message = R.string.home_loading;
        } else if (data.owner == null) {
            message = R.string.home_login_needed;
        } else if (failed) {
            message = R.string.home_load_issue;
        }
        TextView status = view.findViewById(R.id.home_data_status);
        status.setVisibility(message == 0 ? View.GONE : View.VISIBLE);
        if (message != 0) {
            status.setText(message);
        }
        view.findViewById(R.id.home_data_retry).setVisibility(
                !data.loading && !model.isDemo() ? View.VISIBLE : View.GONE);
        TextView demo = view.findViewById(R.id.home_demo_action);
        demo.setVisibility(model.demoAvailable() ? View.VISIBLE : View.GONE);
        demo.setText(model.isDemo() ? R.string.home_demo_disable : R.string.home_demo_enable);
    }
}
