package com.team.mogolog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.buttonLogout)
                .setOnClickListener(button -> {
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(
                            requireContext(),
                            LoginActivity.class
                    );

                    // 홈 화면 기록을 지워 뒤로가기로 돌아가지 못하게 처리
                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    requireActivity().finish();
                });
    }
}