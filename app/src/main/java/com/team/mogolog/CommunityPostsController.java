package com.team.mogolog;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.team.mogolog.model.FirestorePaths;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** CommunityFragment owns this controller for the lifetime of its view. */
public final class CommunityPostsController {
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final LinearLayout list;
    private final TextView status;
    private final Button write;
    private ListenerRegistration registration;
    private BottomSheetDialog composer;
    private AlertDialog preview;
    private boolean active;
    private boolean destroyed;
    private long revision;
    private String examId;
    private String examName;
    private String subject;
    private String elective;
    private int question;
    private final List<Integer> allowedNumbers = new ArrayList<>();

    public CommunityPostsController(View root) {
        context = root.getContext();
        list = root.findViewById(R.id.communityPosts);
        status = root.findViewById(R.id.textCommunityStatus);
        write = root.findViewById(R.id.buttonCommunityWrite);
        write.setOnClickListener(v -> compose());
        clear();
    }

    public void select(String examId, String examName, String subject,
                       String elective, int question, List<Integer> numbers) {
        this.examId = examId;
        this.examName = examName;
        this.subject = subject;
        this.elective = elective;
        this.question = question;
        allowedNumbers.clear();
        for (Integer number : numbers) {
            if (number != null && number > 0) allowedNumbers.add(number);
        }
        write.setEnabled(!allowedNumbers.isEmpty());
        listen();
    }

    public void clear() {
        stopListener();
        examId = null;
        allowedNumbers.clear();
        list.removeAllViews();
        write.setEnabled(false);
    }

    public void setActive(boolean value) {
        if (destroyed || active == value) return;
        active = value;
        if (active) listen(); else stopListener();
    }

    private void stopListener() {
        revision++;
        if (registration != null) registration.remove();
        registration = null;
    }

    private String scopeKey(String exam, String subj, String elect, int number) {
        return encode(exam) + "." + encode(subj) + "."
                + encode(elect == null ? "" : elect) + "." + number;
    }

    private String encode(String value) {
        return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private void listen() {
        stopListener();
        list.removeAllViews();
        if (!active || examId == null || allowedNumbers.isEmpty()) return;
        final long request = revision;
        status.setText("질문을 불러오는 중입니다.");
        registration = db.collection("posts")
                .whereArrayContains("scopeKeys", scopeKey(examId, subject, elective, question))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshot, error) -> {
                    if (destroyed || !active || request != revision) return;
                    list.removeAllViews();
                    if (error != null) {
                        status.setText("질문 조회 실패\n" + error.getLocalizedMessage());
                        return;
                    }
                    if (snapshot == null || snapshot.isEmpty()) {
                        status.setText("아직 등록된 질문이 없습니다. 첫 질문을 남겨보세요.");
                        return;
                    }
                    status.setText("최근 질문 " + snapshot.size() + "개 (최대 50개)");
                    for (DocumentSnapshot post : snapshot.getDocuments()) addPost(post);
                });
    }

    private String string(DocumentSnapshot post, String key) {
        String value = post.getString(key);
        return value == null ? "" : value;
    }

    private void addPost(DocumentSnapshot post) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(16), dp(12), dp(16));
        Long number = post.getLong("questionNumber");
        TextView title = label((number == null ? "" : number + "번 · ")
                + string(post, "title"), 16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(title);
        String date = "전송 중";
        Timestamp timestamp = post.getTimestamp("createdAt");
        if (timestamp != null) {
            date = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREA)
                    .format(timestamp.toDate());
        }
        row.addView(label(string(post, "nickname") + " · " + date, 12));
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setOnClickListener(v -> {
            preview = new AlertDialog.Builder(context)
                    .setTitle(string(post, "title"))
                    .setMessage(string(post, "nickname") + "\n\n" + string(post, "content"))
                    .setPositiveButton("닫기", null)
                    .create();
            preview.show();
        });
        list.addView(row);
        View divider = new View(context);
        divider.setBackgroundColor(0xFFE0E0E0);
        list.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void compose() {
        if (examId == null || allowedNumbers.isEmpty()
                || (composer != null && composer.isShowing())) return;
        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            toast("로그인 후 질문을 작성해주세요.");
            return;
        }
        // 입력창을 연 시점의 시험·과목을 저장한다.
        final String targetExam = examId;
        final String targetName = examName;
        final String targetSubject = subject;
        final String targetElective = elective;
        final List<Integer> numbers = new ArrayList<>(allowedNumbers);
        final DocumentReference postRef = db.collection("posts").document();

        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        composer = dialog;
        ScrollView scroll = new ScrollView(context);
        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(24), dp(24), dp(32));
        scroll.addView(form);
        form.addView(label("질문 작성", 20));
        form.addView(label(targetName + "\n" + targetSubject + " · "
                + (targetElective == null ? "공통" : targetElective), 14));
        form.addView(label("문항 번호", 14));
        Spinner numberPicker = new Spinner(context);
        List<String> labels = new ArrayList<>();
        for (int number : numbers) labels.add(number + "번");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numberPicker.setAdapter(adapter);
        numberPicker.setMinimumHeight(dp(48));
        int initial = numbers.indexOf(question);
        if (initial >= 0) numberPicker.setSelection(initial);
        form.addView(numberPicker);

        EditText title = new EditText(context);
        title.setHint("질문 제목 (최대 100자)");
        title.setSingleLine(true);
        title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        form.addView(title);
        EditText content = new EditText(context);
        content.setHint("어떤 부분이 궁금한가요? (최대 2000자)");
        content.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        content.setMinLines(4);
        content.setGravity(android.view.Gravity.TOP);
        content.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
        form.addView(content);
        TextView errorText = label("문제 사진은 공유하지 않고 글로만 질문해주세요.", 12);
        form.addView(errorText);
        Button submit = new Button(context);
        submit.setText("질문 등록");
        form.addView(submit);
        dialog.setContentView(scroll);
        dialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();

        submit.setOnClickListener(v -> {
            String titleValue = title.getText().toString().trim();
            String contentValue = content.getText().toString().trim();
            if (titleValue.isEmpty()) { title.setError("제목을 입력해주세요."); return; }
            if (contentValue.isEmpty()) { content.setError("내용을 입력해주세요."); return; }
            if (!uid.equals(FirebaseAuth.getInstance().getUid())) {
                errorText.setText("로그인 상태가 변경되었습니다. 다시 로그인해주세요.");
                return;
            }
            int number = numbers.get(numberPicker.getSelectedItemPosition());
            submit.setEnabled(false);
            title.setEnabled(false);
            content.setEnabled(false);
            numberPicker.setEnabled(false);
            submit.setText("등록 중...");
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            db.collection(FirestorePaths.USERS).document(uid).get()
                    .addOnCompleteListener(profileTask -> {
                        if (destroyed) return;
                        String nickname = profileTask.isSuccessful()
                                ? profileTask.getResult().getString("nickname") : null;
                        if (nickname == null || nickname.trim().isEmpty()) {
                            restoreForm(dialog, submit, title, content, numberPicker);
                            errorText.setText("닉네임을 불러오지 못했습니다. 프로필과 연결을 확인 후 다시 눌러주세요.");
                            return;
                        }
                        Map<String, Object> data = new HashMap<>();
                        data.put("uid", uid);
                        data.put("nickname", nickname);
                        data.put("examId", targetExam);
                        data.put("subject", targetSubject);
                        data.put("elective", targetElective);
                        data.put("questionNumber", number);
                        data.put("title", titleValue);
                        data.put("content", contentValue);
                        data.put("createdAt", FieldValue.serverTimestamp());
                        List<String> keys = new ArrayList<>();
                        keys.add(scopeKey(targetExam, targetSubject, targetElective, 0));
                        keys.add(scopeKey(targetExam, targetSubject, targetElective, number));
                        data.put("scopeKeys", keys);
                        // 같은 입력창에서 재시도하면 같은 문서에 저장해 중복 등록 방지
                        postRef.set(data).addOnCompleteListener(saveTask -> {
                            if (destroyed) return;
                            if (saveTask.isSuccessful()) {
                                dialog.dismiss();
                                toast("질문이 등록되었습니다.");
                            } else {
                                restoreForm(dialog, submit, title, content, numberPicker);
                                errorText.setText("등록 실패: " + (saveTask.getException() == null
                                        ? "다시 시도해주세요." : saveTask.getException().getLocalizedMessage()));
                            }
                        });
                    });
        });
    }

    private void restoreForm(BottomSheetDialog dialog, Button submit,
                             EditText title, EditText content, Spinner number) {
        submit.setEnabled(true);
        submit.setText("질문 등록");
        title.setEnabled(true);
        content.setEnabled(true);
        number.setEnabled(true);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
    }

    private TextView label(String text, int size) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setPadding(0, dp(4), 0, dp(4));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public void destroy() {
        destroyed = true;
        stopListener();
        if (composer != null) composer.dismiss();
        if (preview != null) preview.dismiss();
        write.setOnClickListener(null);
    }
}
