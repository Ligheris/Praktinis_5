package com.mipt.notes.storage;

import android.content.Context;
import android.util.Log;

import com.mipt.notes.model.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Secondary note repository backed by a JSON file in internal app storage.
 */
public class FileNoteRepository implements NoteRepository {

    private static final String TAG = "FileNoteRepository";
    private static final String FILE_NAME = "notes_storage.json";

    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_UPDATED_AT = "updatedAt";

    private final File storageFile;

    public FileNoteRepository(Context context) {
        this(new File(context.getApplicationContext().getFilesDir(), FILE_NAME));
    }

    public FileNoteRepository(File storageFile) {
        this.storageFile = storageFile;
    }

    @Override
    public synchronized List<Note> getAllNotes() {
        List<Note> notes = readAllFromFile();
        Collections.sort(notes, Comparator.comparingLong(Note::getUpdatedAt).reversed());
        return notes;
    }

    @Override
    public synchronized Note getNoteById(String id) {
        if (id == null) {
            return null;
        }
        for (Note note : readAllFromFile()) {
            if (id.equals(note.getId())) {
                return note;
            }
        }
        return null;
    }

    @Override
    public synchronized void saveNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note must not be null");
        }
        List<Note> notes = readAllFromFile();
        boolean updated = false;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId().equals(note.getId())) {
                notes.set(i, note);
                updated = true;
                break;
            }
        }
        if (!updated) {
            notes.add(note);
        }
        writeAllToFile(notes);
    }

    @Override
    public synchronized void deleteNote(String id) {
        if (id == null) {
            return;
        }
        List<Note> notes = readAllFromFile();
        for (int i = 0; i < notes.size(); i++) {
            if (id.equals(notes.get(i).getId())) {
                notes.remove(i);
                break;
            }
        }
        writeAllToFile(notes);
    }

    @Override
    public synchronized void replaceAllNotes(List<Note> notes) {
        writeAllToFile(notes == null ? Collections.emptyList() : new ArrayList<>(notes));
    }

    private List<Note> readAllFromFile() {
        List<Note> notes = new ArrayList<>();
        if (!storageFile.exists()) {
            return notes;
        }

        try (FileInputStream fis = new FileInputStream(storageFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            String jsonText = baos.toString(StandardCharsets.UTF_8.name()).trim();
            if (jsonText.isEmpty()) {
                return notes;
            }
            JSONArray array = new JSONArray(jsonText);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.getString(KEY_ID);
                String title = obj.optString(KEY_TITLE, "");
                String content = obj.optString(KEY_CONTENT, "");
                long updatedAt = obj.getLong(KEY_UPDATED_AT);
                notes.add(new Note(id, title, content, updatedAt));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to read notes from file", e);
            throw new IllegalStateException("Failed to read notes from file", e);
        }
        return notes;
    }

    private void writeAllToFile(List<Note> notes) {
        JSONArray array = new JSONArray();
        for (Note note : notes) {
            if (note == null) {
                continue;
            }
            try {
                JSONObject obj = new JSONObject();
                obj.put(KEY_ID, note.getId());
                obj.put(KEY_TITLE, note.getTitle() != null ? note.getTitle() : "");
                obj.put(KEY_CONTENT, note.getContent() != null ? note.getContent() : "");
                obj.put(KEY_UPDATED_AT, note.getUpdatedAt());
                array.put(obj);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize note", e);
                throw new IllegalStateException("Failed to serialize notes", e);
            }
        }

        File parent = storageFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IllegalStateException("Failed to create storage directory: " + parent.getAbsolutePath());
        }

        File tempFile = parent != null
                ? new File(parent, storageFile.getName() + ".tmp")
                : new File(storageFile.getName() + ".tmp");

        try {
            byte[] payload = array.toString(2).getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream fos = new FileOutputStream(tempFile, false)) {
                fos.write(payload);
                fos.flush();
                fos.getFD().sync();
            }
            replaceTargetFile(tempFile);
        } catch (IOException | JSONException e) {
            cleanupTempFile(tempFile);
            Log.e(TAG, "Failed to write notes to file", e);
            throw new IllegalStateException("Failed to write notes to file", e);
        }
    }

    private void replaceTargetFile(File tempFile) throws IOException {
        if (tempFile.renameTo(storageFile)) {
            return;
        }
        // Fallback for platforms (e.g., Windows) where renameTo fails when destination exists.
        if (storageFile.exists() && !storageFile.delete()) {
            throw new IOException("Failed to delete existing notes file before replacement");
        }
        if (!tempFile.renameTo(storageFile)) {
            throw new IOException("Failed to move temporary notes file to target");
        }
    }

    private void cleanupTempFile(File tempFile) {
        if (tempFile.exists() && !tempFile.delete()) {
            Log.w(TAG, "Failed to delete temporary file: " + tempFile.getAbsolutePath());
        }
    }
}
