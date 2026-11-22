package com.team.mogolog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.team.mogolog.model.Exam;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Local review queue opened from Home. The records and community tabs own their own screens. */
public class HomeReviewDialog extends DialogFragment {
    static final String TAG = "home_review";
    private static final String STATE_PENDING_ONLY = "pending_only";
    private boolean pendingOnly = true;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pendingOnly = savedInstanceState.getBoolean(STATE_PENDING_ONLY, true);
        }
        HomeDataModel model = new ViewModelProvider(requireParentFragment()).get(HomeDataModel.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = android.view.LayoutInflater.from(builder.getContext())
                .inflate(R.layout.dialog_home_review, null);
        CheckBox filter = view.findViewById(R.id.home_review_pending_filter);
        ListView list = view.findViewById(R.id.home_review_list);
        TextView empty = view.findViewById(R.id.home_review_empty);
        filter.setSaveEnabled(false);
        filter.setChecked(pendingOnly);

        List<HomeData.ReviewItem> visible = new ArrayList<>();
        ArrayAdapter<String> rows = new ArrayAdapter<>(builder.getContext(), android.R.layout.simple_list_item_1);
        list.setAdapter(rows);
        list.setEmptyView(empty);
        Runnable render = () -> renderQueue(model.current(), visible, rows, empty);
        filter.setOnCheckedChangeListener((button, checked) -> {
            pendingOnly = checked;
            render.run();
        });
        model.data().observe(this, data -> render.run());
        list.setOnItemClickListener((parent, row, position, id) -> {
            HomeData data = model.current();
            if (position >= visible.size() || data.owner == null
                    || !Objects.equals(data.owner, model.owner())
                    || getParentFragmentManager().isStateSaved()
                    || getParentFragmentManager().findFragmentByTag(QuestionDetailFragment.TAG) != null) {
                return;
            }
            QuestionDetailFragment.forItem(data, visible.get(position))
                    .showNow(getParentFragmentManager(), QuestionDetailFragment.TAG);
        });
        return builder.setTitle(R.string.home_review_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.home_review_close, null)
                .create();
    }

    private void renderQueue(HomeData data, List<HomeData.ReviewItem> visible,
                             ArrayAdapter<String> rows, TextView empty) {
        visible.clear();
        List<String> labels = new ArrayList<>();
        if (data.owner != null && data.scoresAvailable && (!pendingOnly || data.notesAvailable)) {
            ReviewDrafts drafts = new ReviewDrafts(requireContext(), data.owner);
            for (HomeData.ReviewItem item : data.reviewItems(drafts::done)) {
                if (pendingOnly && item.done) {
                    continue;
                }
                visible.add(item);
                Exam exam = data.exam(item.score.getExamId());
                String examName = exam != null && exam.getName() != null
                        ? exam.getName() : item.score.getExamId();
                String state = getString(item.done ? R.string.question_review_done : R.string.review_pending);
                if (!data.notesAvailable && drafts.done(item.noteId) == null) {
                    state = getString(R.string.value_placeholder);
                }
                labels.add(getString(R.string.review_row, examName, item.score.getSubject(),
                        item.number, state));
            }
        }
        if (data.loading) {
            empty.setText(R.string.home_loading);
        } else if (data.owner == null) {
            empty.setText(R.string.home_login_needed);
        } else if (!data.scoresAvailable || (pendingOnly && !data.notesAvailable)) {
            empty.setText(R.string.review_load_issue);
        } else {
            empty.setText(R.string.review_no_items);
        }
        rows.setNotifyOnChange(false);
        rows.clear();
        rows.addAll(labels);
        rows.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PENDING_ONLY, pendingOnly);
    }
}
