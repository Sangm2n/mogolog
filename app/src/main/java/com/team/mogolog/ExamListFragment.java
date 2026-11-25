package com.team.mogolog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** Exam records screen placeholder. Feature owner: 이상민. */
public class ExamListFragment extends Fragment {
    public ExamListFragment() {
        super(R.layout.fragment_exam_list);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.exam_list_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(requireActivity(), ExamPickActivity.class));
            }
        });

    }
}
