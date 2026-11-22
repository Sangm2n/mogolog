package com.team.mogolog;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class ReviewDrafts {
    private final SharedPreferences preferences;
    private final File photoDirectory;
    private final String owner;

    ReviewDrafts(Context context, String owner) {
        this.owner = owner;
        preferences = context.getSharedPreferences("question_review_drafts", Context.MODE_PRIVATE);
        photoDirectory = new File(context.getNoBackupFilesDir(), "question_review_photos");
    }

    private String key(String noteId) {
        return UUID.nameUUIDFromBytes((owner + "\n" + noteId)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    Boolean done(String noteId) {
        String field = key(noteId) + ".done";
        return preferences.contains(field) ? preferences.getBoolean(field, false) : null;
    }

    String comment(String noteId, String fallback) {
        return preferences.getString(key(noteId) + ".comment", fallback);
    }

    void saveDone(String noteId, boolean done) {
        preferences.edit().putBoolean(key(noteId) + ".done", done).apply();
    }

    void saveComment(String noteId, String comment) {
        preferences.edit().putString(key(noteId) + ".comment", comment).apply();
    }

    Bitmap photo(String noteId) {
        File file = photoFile(noteId);
        try (java.io.FileInputStream input = new AtomicFile(file).openRead()) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException failure) {
            return null;
        }
    }

    boolean savePhoto(String noteId, Bitmap photo) {
        if (!photoDirectory.isDirectory() && !photoDirectory.mkdirs()) {
            return false;
        }
        AtomicFile destination = new AtomicFile(photoFile(noteId));
        FileOutputStream output = null;
        try {
            output = destination.startWrite();
            if (!photo.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                destination.failWrite(output);
                return false;
            }
            destination.finishWrite(output);
            return true;
        } catch (IOException failure) {
            destination.failWrite(output);
            return false;
        }
    }

    private File photoFile(String noteId) {
        return new File(photoDirectory, key(noteId) + ".jpg");
    }
}
