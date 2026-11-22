package com.team.mogolog;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.mogolog.model.FirestorePaths;
import com.team.mogolog.model.User;

public class SignUpActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editPassword;
    private EditText editPasswordConfirm;
    private EditText editNickname;
    private CheckBox checkStats;
    private Button buttonSignUp;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // 이 화면에서 새로 생성한 계정의 UID
    private String pendingUid;
    private boolean isSubmitting = false;
    private boolean isCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editPasswordConfirm = findViewById(R.id.editPasswordConfirm);
        editNickname = findViewById(R.id.editNickname);
        checkStats = findViewById(R.id.checkStats);
        buttonSignUp = findViewById(R.id.buttonSignUp);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets safeArea = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout()
                                    | WindowInsetsCompat.Type.ime()
                    );

                    view.setPadding(
                            safeArea.left,
                            safeArea.top,
                            safeArea.right,
                            safeArea.bottom
                    );

                    return insets;
                }
        );

        buttonSignUp.setOnClickListener(view -> submitSignUp());
    }

    private void submitSignUp() {
        if (isSubmitting || isCompleted) {
            return;
        }

        String nickname = editNickname.getText().toString().trim();
        boolean statsAgreed = checkStats.isChecked();

        if (nickname.isEmpty()) {
            showError(editNickname, "닉네임을 입력해주세요.");
            return;
        }

        User profile = new User();
        profile.setNickname(nickname);
        profile.setStatsAgreed(statsAgreed);

        // 계정 생성은 성공했고 프로필 저장만 실패했다면 저장 재시도
        if (pendingUid != null) {
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser == null
                    || !pendingUid.equals(currentUser.getUid())) {
                showMessage("로그인 상태가 변경됐습니다. 다시 로그인해주세요.");
                return;
            }

            setSubmitting(true);
            saveProfile(profile);
            return;
        }

        String email = editEmail.getText().toString().trim();

        // 비밀번호는 공백도 입력값이므로 trim()을 사용하지 않음
        String password = editPassword.getText().toString();
        String passwordConfirm =
                editPasswordConfirm.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(editEmail, "올바른 이메일을 입력해주세요.");
            return;
        }

        if (password.isEmpty()) {
            showError(editPassword, "비밀번호를 입력해주세요.");
            return;
        }

        if (passwordConfirm.isEmpty()) {
            showError(editPasswordConfirm, "비밀번호를 다시 입력해주세요.");
            return;
        }

        if (!password.equals(passwordConfirm)) {
            showError(editPasswordConfirm, "비밀번호가 일치하지 않습니다.");
            return;
        }

        setSubmitting(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setSubmitting(false);
                        showFailure("계정 생성 실패", task.getException());
                        return;
                    }

                    FirebaseUser account = task.getResult().getUser();

                    if (account == null) {
                        setSubmitting(false);
                        showMessage("생성한 계정 정보를 확인할 수 없습니다.");
                        return;
                    }

                    pendingUid = account.getUid();

                    // 계정 생성 후에는 이메일·비밀번호 변경 방지
                    editEmail.setEnabled(false);
                    editPassword.setEnabled(false);
                    editPasswordConfirm.setEnabled(false);

                    saveProfile(profile);
                });
    }

    private void saveProfile(User profile) {
        // UID는 문서 이름으로 사용.
        // User의 @DocumentId 필드에는 따로 값을 넣지 않음.
        db.collection(FirestorePaths.USERS)
                .document(pendingUid)
                .set(profile)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        isCompleted = true;
                        isSubmitting = false;

                        buttonSignUp.setText("가입 완료");
                        buttonSignUp.setEnabled(false);

                        editNickname.setEnabled(false);
                        checkStats.setEnabled(false);

                        editPassword.setText("");
                        editPasswordConfirm.setText("");

                        // 가입 시 자동 로그인된 상태를 해제
                        auth.signOut();

                        showMessage("회원가입이 완료되었습니다. 로그인해주세요.");

// 회원가입 화면을 닫으면 이전 로그인 화면이 나타남
                        finish();
                    } else {
                        setSubmitting(false);
                        buttonSignUp.setText("사용자 정보 저장 재시도");

                        showFailure(
                                "계정은 생성됐지만 사용자 정보 저장에 실패했습니다. "
                                        + "이 화면에서 다시 눌러주세요.",
                                task.getException()
                        );
                    }
                });
    }

    private void setSubmitting(boolean submitting) {
        isSubmitting = submitting;
        buttonSignUp.setEnabled(!submitting);

        if (submitting) {
            buttonSignUp.setText("처리 중...");
        } else if (pendingUid != null) {
            buttonSignUp.setText("사용자 정보 저장 재시도");
        } else {
            buttonSignUp.setText("회원가입");
        }

        editNickname.setEnabled(!submitting);
        checkStats.setEnabled(!submitting);

        boolean canEditAccount = !submitting && pendingUid == null;
        editEmail.setEnabled(canEditAccount);
        editPassword.setEnabled(canEditAccount);
        editPasswordConfirm.setEnabled(canEditAccount);
    }

    private void showError(EditText input, String message) {
        input.setError(message);
        input.requestFocus();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showFailure(String title, Exception exception) {
        String detail = exception != null
                ? exception.getLocalizedMessage()
                : "알 수 없는 오류";

        showMessage(title + "\n" + detail);
    }
}