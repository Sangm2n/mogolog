package com.team.mogolog;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editPassword;
    private Button buttonLogin;
    private Button buttonGoSignUp;

    private FirebaseAuth auth;
    private boolean isLoggingIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        // 이미 로그인된 사용자는 홈으로 이동
        if (auth.getCurrentUser() != null) {
            openHome();
            return;
        }

        editEmail = findViewById(R.id.editLoginEmail);
        editPassword = findViewById(R.id.editLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoSignUp = findViewById(R.id.buttonGoSignUp);

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

        buttonLogin.setOnClickListener(view -> login());

        buttonGoSignUp.setOnClickListener(view ->
                startActivity(new Intent(
                        LoginActivity.this,
                        SignUpActivity.class
                ))
        );
    }

    private void login() {
        if (isLoggingIn) {
            return;
        }

        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("올바른 이메일을 입력해주세요.");
            editEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editPassword.setError("비밀번호를 입력해주세요.");
            editPassword.requestFocus();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        editPassword.setText("");
                        openHome();
                    } else {
                        Toast.makeText(
                                LoginActivity.this,
                                "로그인에 실패했습니다. 이메일·비밀번호와 "
                                        + "네트워크 연결을 확인해주세요.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openHome() {
        Intent intent = new Intent(
                LoginActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        isLoggingIn = loading;

        buttonLogin.setEnabled(!loading);
        buttonGoSignUp.setEnabled(!loading);
        editEmail.setEnabled(!loading);
        editPassword.setEnabled(!loading);

        buttonLogin.setText(loading ? "로그인 중..." : "로그인");
    }
}