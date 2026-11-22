package com.team.mogolog;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.team.mogolog.model.Exam;
import com.team.mogolog.model.FirestorePaths;
import com.team.mogolog.model.Note;
import com.team.mogolog.model.Score;
import com.team.mogolog.model.Subjects;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public final class HomeDataModel extends AndroidViewModel {
    private static final String DEMO_OWNER = "local-demo";
    private final MutableLiveData<HomeData> data = new MutableLiveData<>(HomeData.empty(null, true));
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseAuth.AuthStateListener authListener = ignored -> onAuthChanged();
    private boolean demo;
    private int loadVersion;

    public HomeDataModel(@NonNull Application application) {
        super(application);
        auth.addAuthStateListener(authListener);
    }

    LiveData<HomeData> data() {
        return data;
    }

    HomeData current() {
        return Objects.requireNonNull(data.getValue());
    }

    boolean demoAvailable() {
        return (getApplication().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    boolean isDemo() {
        return demo;
    }

    void toggleDemo() {
        if (demoAvailable()) {
            demo = !demo;
            reload();
        }
    }

    void draftChanged() {
        data.setValue(current());
    }

    String owner() {
        return demo ? DEMO_OWNER : auth.getUid();
    }

    private void onAuthChanged() {
        if (!demo) {
            reload();
        }
    }

    void reload() {
        int version = ++loadVersion;
        if (demo) {
            data.setValue(demoData());
            return;
        }
        String uid = auth.getUid();
        data.setValue(HomeData.empty(uid, true));
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> exams = db.collection(FirestorePaths.EXAMS).get();
        Task<QuerySnapshot> scores = uid == null ? Tasks.forResult(null)
                : db.collection(FirestorePaths.SCORES).whereEqualTo("uid", uid).get();
        Task<QuerySnapshot> notes = uid == null ? Tasks.forResult(null)
                : db.collection(FirestorePaths.NOTES).whereEqualTo("uid", uid).get();

        Tasks.whenAllComplete(exams, scores, notes).addOnCompleteListener(ignored -> {
            if (version != loadVersion || !Objects.equals(uid, auth.getUid())) {
                return;
            }
            data.setValue(new HomeData(uid, read(exams, Exam.class), read(scores, Score.class),
                    read(notes, Note.class), exams.isSuccessful(),
                    uid != null && scores.isSuccessful(), uid != null && notes.isSuccessful(), false));
        });
    }

    private static <T> List<T> read(Task<QuerySnapshot> task, Class<T> type) {
        if (!task.isSuccessful() || task.getResult() == null) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        for (DocumentSnapshot document : task.getResult()) {
            try {
                T value = document.toObject(type);
                if (value != null) {
                    result.add(value);
                }
            } catch (RuntimeException malformedDocument) {
                // One older or malformed record should not hide the user's other records.
            }
        }
        return result;
    }

    private static HomeData demoData() {
        List<Exam> exams = new ArrayList<>();
        List<Score> scores = new ArrayList<>();
        List<Note> notes = new ArrayList<>();
        int[][] points = {{72, 78, 75, 81, 84}, {72, 76, 74, 84, 86}, {82, 80, 85, 88, 90}};
        String[] subjects = Arrays.copyOf(Subjects.COMMON, points.length);
        Calendar current = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        date.setTimeZone(current.getTimeZone());

        for (int index = 0; index < 5; index++) {
            Calendar taken = (Calendar) current.clone();
            taken.add(Calendar.MONTH, index - 5);
            String examId = "demo-" + index;
            Exam exam = new Exam();
            exam.setId(examId);
            exam.setName("예시 모의고사 " + (index + 1) + "회");
            exam.setType("평가원");
            exam.setDate(date.format(taken.getTime()));
            exams.add(exam);

            for (int subject = 0; subject < subjects.length; subject++) {
                Score score = new Score();
                score.setUid(DEMO_OWNER);
                score.setExamId(examId);
                score.setSubject(subjects[subject]);
                score.setRawScore(points[subject][index]);
                if (index == 4) {
                    score.setWrongNumbers(Arrays.asList(14, 21, 29));
                }
                scores.add(score);
            }
        }

        Note done = new Note();
        done.setUid(DEMO_OWNER);
        done.setExamId("demo-4");
        done.setSubject(Subjects.COMMON[1]);
        done.setQuestionNumber(14);
        done.setDone(true);
        notes.add(done);

        current.add(Calendar.DATE, 5);
        Exam next = new Exam();
        next.setId("demo-next");
        next.setName("예시 전국연합학력평가");
        next.setType("교육청");
        next.setDate(date.format(current.getTime()));
        exams.add(next);
        return new HomeData(DEMO_OWNER, exams, scores, notes, true, true, true, false);
    }

    @Override
    protected void onCleared() {
        loadVersion++;
        auth.removeAuthStateListener(authListener);
        super.onCleared();
    }
}
