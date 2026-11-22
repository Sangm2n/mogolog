package com.team.mogolog;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.team.mogolog.model.FirestorePaths;
import com.team.mogolog.model.Note;

public class QuestionDetailFragment extends DialogFragment {
    static final String TAG = "question_detail";
    private static final String OWNER = "owner";
    private static final String ELECTIVE = "elective";

    private HomeDataModel model;
    private ReviewDrafts drafts;
    private String owner;
    private String noteId;

    private final ActivityResultLauncher<Void> camera = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), this::onPhotoCaptured);

    public QuestionDetailFragment() {
        super(R.layout.activity_question_detail);
    }

    static QuestionDetailFragment forItem(HomeData data, HomeData.ReviewItem item) {
        Bundle args = new Bundle();
        String examId = item.score.getExamId();
        args.putString(OWNER, data.owner);
        args.putString(FirestorePaths.EXTRA_EXAM_ID, examId);
        args.putString(FirestorePaths.EXTRA_SUBJECT, item.score.getSubject());
        args.putInt(FirestorePaths.EXTRA_QUESTION_NUMBER, item.number);
        args.putString(ELECTIVE, item.score.getElective());
        QuestionDetailFragment fragment = new QuestionDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Theme_Mogolog);
        model = new ViewModelProvider(requireParentFragment()).get(HomeDataModel.class);
        Bundle args = requireArguments();
        owner = args.getString(OWNER);
        noteId = Note.makeId(owner, args.getString(FirestorePaths.EXTRA_EXAM_ID),
                args.getString(FirestorePaths.EXTRA_SUBJECT),
                args.getInt(FirestorePaths.EXTRA_QUESTION_NUMBER));
        drafts = new ReviewDrafts(requireContext(), owner);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyInsets(view);

        Bundle args = requireArguments();
        int questionNumber = args.getInt(FirestorePaths.EXTRA_QUESTION_NUMBER);
        TextView title = view.findViewById(R.id.question_title);
        title.setText(getString(R.string.question_number_title, questionNumber));
        TextView elective = view.findViewById(R.id.question_elective);
        String electiveName = args.getString(ELECTIVE);
        elective.setText(electiveName);
        elective.setVisibility(electiveName == null || electiveName.isEmpty() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.question_error_rate).setVisibility(View.GONE);
        view.findViewById(R.id.question_back).setOnClickListener(button -> dismiss());
        view.findViewById(R.id.question_capture_photo).setOnClickListener(button -> launchCamera());
        view.findViewById(R.id.question_community_action).setVisibility(View.GONE);

        TextView comment = view.findViewById(R.id.question_comment);
        SwitchCompat done = view.findViewById(R.id.question_review_done);
        // Disable widget state restoration: the account's draft is the source of truth.
        comment.setSaveEnabled(false);
        done.setSaveEnabled(false);
        comment.setEnabled(false);
        done.setEnabled(false);
        model.data().observe(getViewLifecycleOwner(), data -> {
            if (!canEdit()) {
                dismissAllowingStateLoss();
                return;
            }
            if (!data.loading) {
                loadNote(data, comment, done);
            }
        });
        if (canEdit()) {
            showPhoto(view, drafts.photo(noteId));
        }
    }

    private void loadNote(HomeData data, TextView comment, SwitchCompat done) {
        if (comment.isEnabled()) {
            return;
        }
        Note remote = data.note(noteId);
        comment.setText(drafts.comment(noteId, remote != null ? remote.getComment() : null));
        Boolean localDone = drafts.done(noteId);
        done.setChecked(localDone != null ? localDone : remote != null && remote.isDone());

        comment.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable text) {
                if (canEdit()) {
                    drafts.saveComment(noteId, text.toString());
                }
            }
        });
        done.setOnCheckedChangeListener((button, checked) -> {
            if (canEdit()) {
                drafts.saveDone(noteId, checked);
                model.draftChanged();
            }
        });
        comment.setEnabled(true);
        done.setEnabled(true);
    }

    private boolean canEdit() {
        return owner != null && owner.equals(model.owner());
    }

    private void launchCamera() {
        if (!canEdit()) {
            return;
        }
        try {
            camera.launch(null);
        } catch (ActivityNotFoundException failure) {
            Toast.makeText(requireContext(), R.string.question_camera_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void onPhotoCaptured(Bitmap photo) {
        if (photo == null || !canEdit()) {
            return;
        }
        if (!drafts.savePhoto(noteId, photo)) {
            Toast.makeText(requireContext(), R.string.question_photo_save_error, Toast.LENGTH_SHORT).show();
            return;
        }
        View view = getView();
        if (view != null) {
            showPhoto(view, photo);
        }
    }

    private void showPhoto(View view, Bitmap photo) {
        boolean hasPhoto = photo != null;
        ImageView preview = view.findViewById(R.id.question_photo);
        preview.setImageBitmap(photo);
        preview.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.question_photo_label).setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.question_photo_privacy_label).setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        View controls = view.findViewById(R.id.question_photo_placeholder);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) controls.getLayoutParams();
        params.width = hasPhoto ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = hasPhoto ? Gravity.BOTTOM | Gravity.END : Gravity.CENTER;
        controls.setLayoutParams(params);
        TextView capture = view.findViewById(R.id.question_capture_photo);
        capture.setText(hasPhoto ? R.string.question_retake_photo : R.string.question_take_photo);
    }

    private void applyInsets(View view) {
        int left = view.getPaddingLeft();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (target, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            target.setPadding(left + bars.left, bars.top, right + bars.right,
                    bottom + Math.max(bars.bottom, ime));
            return insets;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(true);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightNavigationBars(true);
            ViewCompat.requestApplyInsets(requireView());
        }
    }
}
