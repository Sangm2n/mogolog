package com.team.mogolog;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.mogolog.model.FirestorePaths;
import com.team.mogolog.model.User;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final String NICKNAMES = "nicknames";
    private static final String NICKNAME_TAKEN = "NICKNAME_TAKEN";

    private EditText editEmail;
    private EditText editPassword;
    private EditText editPasswordConfirm;
    private EditText editNickname;

    private CheckBox checkStats;
    private Button buttonSignUp;
    private Button buttonCheckNickname;
    private TextView textNicknameStatus;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String pendingUid;
    private String checkedNickname;

    private long nicknameRevision = 0;

    private boolean isCheckingNickname = false;
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
        buttonCheckNickname = findViewById(R.id.buttonCheckNickname);
        textNicknameStatus = findViewById(R.id.textNicknameStatus);

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

        editNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                nicknameRevision++;
                checkedNickname = null;
                editNickname.setError(null);

                textNicknameStatus.setText(
                        "닉네임 중복 확인을 해주세요."
                );

                updateControls();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        buttonCheckNickname.setOnClickListener(
                view -> checkNickname()
        );

        buttonSignUp.setOnClickListener(
                view -> submitSignUp()
        );

        updateControls();
    }

    // 닉네임에 / 등의 문자가 있어도 문서 이름으로 사용할 수 있게 변환
    private String nicknameId(String nickname) {
        return Base64.encodeToString(
                nickname.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING
        );
    }

    private boolean validateNickname(String nickname) {
        if (nickname.isEmpty()) {
            showError(editNickname, "닉네임을 입력해주세요.");
            return false;
        }

        int length = nickname.codePointCount(0, nickname.length());

        if (length > 20) {
            showError(editNickname, "닉네임은 20자 이하로 입력해주세요.");
            return false;
        }

        return true;
    }

    private void checkNickname() {
        if (isCheckingNickname || isSubmitting || isCompleted) {
            return;
        }

        String nickname = editNickname.getText().toString().trim();

        if (!validateNickname(nickname)) {
            return;
        }

        long requestRevision = nicknameRevision;

        checkedNickname = null;
        isCheckingNickname = true;
        textNicknameStatus.setText("닉네임을 확인하고 있습니다.");
        updateControls();

        // 가입 전에는 사용자 정보 대신 닉네임 문서 하나만 조회
        db.collection(NICKNAMES)
                .document(nicknameId(nickname))
                .get()
                .addOnCompleteListener(this, task -> {
                    isCheckingNickname = false;

                    if (requestRevision != nicknameRevision) {
                        textNicknameStatus.setText(
                                "닉네임이 변경되었습니다. 다시 확인해주세요."
                        );
                        updateControls();
                        return;
                    }

                    if (!task.isSuccessful()) {
                        textNicknameStatus.setText(
                                "확인에 실패했습니다. 다시 시도해주세요."
                        );
                        updateControls();
                        showFailure("닉네임 확인 실패", task.getException());
                        return;
                    }

                    DocumentSnapshot document = task.getResult();

                    // 저장 재시도 중 본인이 확보한 닉네임은 허용
                    boolean ownedByPendingUser =
                            pendingUid != null
                                    && pendingUid.equals(
                                    document.getString("uid")
                            );

                    if (!document.exists() || ownedByPendingUser) {
                        checkedNickname = nickname;
                        textNicknameStatus.setText(
                                "사용 가능합니다. 가입 시 최종 확인합니다."
                        );
                        editNickname.setError(null);
                    } else {
                        showNicknameTaken();
                    }

                    updateControls();
                });
    }

    private void submitSignUp() {
        if (isSubmitting || isCompleted) {
            return;
        }

        if (isCheckingNickname) {
            showMessage("닉네임 확인이 끝날 때까지 기다려주세요.");
            return;
        }

        String nickname = editNickname.getText().toString().trim();

        if (!validateNickname(nickname)) {
            return;
        }

        if (!nickname.equals(checkedNickname)) {
            showError(editNickname, "닉네임 중복 확인을 해주세요.");
            return;
        }

        User profile = new User();
        profile.setNickname(nickname);
        profile.setStatsAgreed(checkStats.isChecked());

        // 이미 계정 생성에 성공했다면 프로필 저장만 재시도
        if (pendingUid != null) {
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser == null
                    || !pendingUid.equals(currentUser.getUid())) {
                showMessage(
                        "로그인 상태가 변경됐습니다. 다시 로그인해주세요."
                );
                return;
            }

            setSubmitting(true);
            saveProfile(profile);
            return;
        }

        String email = editEmail.getText().toString().trim();
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
            showError(
                    editPasswordConfirm,
                    "비밀번호를 다시 입력해주세요."
            );
            return;
        }

        if (!password.equals(passwordConfirm)) {
            showError(
                    editPasswordConfirm,
                    "비밀번호가 일치하지 않습니다."
            );
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
                        showMessage(
                                "생성한 계정 정보를 확인할 수 없습니다."
                        );
                        return;
                    }

                    pendingUid = account.getUid();
                    updateControls();

                    saveProfile(profile);
                });
    }

    private void saveProfile(User profile) {
        final String uid = pendingUid;
        final String nickname = profile.getNickname();

        /*
         * 기존 users에만 저장된 닉네임도 확인한다.
         * 이 시점에는 계정 생성 후이므로 인증된 조회가 가능하다.
         */
        db.collection(FirestorePaths.USERS)
                .whereEqualTo("nickname", nickname)
                .limit(2)
                .get()
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        profileSaveFailed(task.getException());
                        return;
                    }

                    for (DocumentSnapshot document
                            : task.getResult().getDocuments()) {
                        if (!uid.equals(document.getId())) {
                            showNicknameTaken();
                            setSubmitting(false);
                            return;
                        }
                    }

                    reserveNicknameAndSave(profile, uid, nickname);
                });
    }

    private void reserveNicknameAndSave(
            User profile,
            String uid,
            String nickname) {

        DocumentReference nicknameRef = db.collection(NICKNAMES)
                .document(nicknameId(nickname));

        DocumentReference userRef = db.collection(FirestorePaths.USERS)
                .document(uid);

        db.runTransaction(transaction -> {
                    DocumentSnapshot nicknameDocument =
                            transaction.get(nicknameRef);

                    if (nicknameDocument.exists()
                            && !uid.equals(
                            nicknameDocument.getString("uid")
                    )) {
                        throw new IllegalStateException(NICKNAME_TAKEN);
                    }

                    if (!nicknameDocument.exists()) {
                        Map<String, Object> reservation = new HashMap<>();
                        reservation.put("uid", uid);
                        reservation.put("nickname", nickname);

                        transaction.set(nicknameRef, reservation);
                    }

                    // 닉네임 확보와 사용자 정보 저장을 함께 처리
                    transaction.set(userRef, profile);

                    return null;
                })
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        if (isNicknameTakenError(task.getException())) {
                            showNicknameTaken();
                            setSubmitting(false);
                        } else {
                            profileSaveFailed(task.getException());
                        }
                        return;
                    }

                    isCompleted = true;
                    isSubmitting = false;
                    updateControls();

                    editPassword.setText("");
                    editPasswordConfirm.setText("");

                    auth.signOut();

                    showMessage(
                            "회원가입이 완료되었습니다. 로그인해주세요."
                    );

                    finish();
                });
    }

    private boolean isNicknameTakenError(Exception exception) {
        Throwable current = exception;

        while (current != null) {
            if (NICKNAME_TAKEN.equals(current.getMessage())) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private void showNicknameTaken() {
        checkedNickname = null;

        textNicknameStatus.setText(
                "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해주세요."
        );

        showError(editNickname, "이미 사용 중인 닉네임입니다.");
    }

    private void profileSaveFailed(Exception exception) {
        setSubmitting(false);

        showFailure(
                "계정은 생성됐지만 사용자 정보 저장에 실패했습니다. "
                        + "이 화면에서 다시 시도해주세요.",
                exception
        );
    }

    private void setSubmitting(boolean submitting) {
        isSubmitting = submitting;
        updateControls();
    }

    private void updateControls() {
        boolean canInteract = !isSubmitting && !isCompleted;

        buttonSignUp.setEnabled(
                canInteract && !isCheckingNickname
        );

        buttonCheckNickname.setEnabled(
                canInteract && !isCheckingNickname
        );

        editNickname.setEnabled(canInteract);
        checkStats.setEnabled(canInteract);

        boolean canEditAccount =
                canInteract && pendingUid == null;

        editEmail.setEnabled(canEditAccount);
        editPassword.setEnabled(canEditAccount);
        editPasswordConfirm.setEnabled(canEditAccount);

        if (isCompleted) {
            buttonSignUp.setText("가입 완료");
        } else if (isSubmitting) {
            buttonSignUp.setText("처리 중...");
        } else if (pendingUid != null) {
            buttonSignUp.setText("사용자 정보 저장 재시도");
        } else {
            buttonSignUp.setText("회원가입");
        }

        if (isCheckingNickname) {
            buttonCheckNickname.setText("확인 중...");
        } else if (checkedNickname != null) {
            buttonCheckNickname.setText("확인 완료");
        } else {
            buttonCheckNickname.setText("중복 확인");
        }
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