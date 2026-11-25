package com.mipt.notes.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mipt.notes.model.Note;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates the active storage backend (SQLite vs File) and synchronization between them.
 */
public class NoteStorageManager {

    public enum StorageType {
        SQLITE,
        FILE
    }

    public interface StoragePreferenceStore {
        StorageType getSelectedStorage();

        void setSelectedStorage(StorageType storageType);
    }

    private static final String PREFS_NAME = "notes_storage_prefs";
    private static final String KEY_ACTIVE_STORAGE = "active_storage";

    private final NoteRepository primaryRepository;
    private final NoteRepository secondaryRepository;
    private final StoragePreferenceStore preferenceStore;

    public NoteStorageManager(Context context) {
        Context appContext = context.getApplicationContext();
        this.primaryRepository = new SqliteNoteRepository(appContext);
        this.secondaryRepository = new FileNoteRepository(appContext);
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.preferenceStore = new StoragePreferenceStore() {
            @Override
            public StorageType getSelectedStorage() {
                String value = prefs.getString(KEY_ACTIVE_STORAGE, StorageType.SQLITE.name());
                try {
                    return StorageType.valueOf(value);
                } catch (IllegalArgumentException e) {
                    return StorageType.SQLITE;
                }
            }

            @Override
            public void setSelectedStorage(StorageType storageType) {
                prefs.edit().putString(KEY_ACTIVE_STORAGE, storageType.name()).apply();
            }
        };
    }

    public NoteStorageManager(
            NoteRepository primaryRepository,
            NoteRepository secondaryRepository,
            StoragePreferenceStore preferenceStore) {
        this.primaryRepository = primaryRepository;
        this.secondaryRepository = secondaryRepository;
        this.preferenceStore = preferenceStore;
    }

    public StorageType getActiveStorageType() {
        return preferenceStore.getSelectedStorage();
    }

    public void setActiveStorageType(StorageType storageType) {
        preferenceStore.setSelectedStorage(storageType);
    }

    public NoteRepository getActiveRepository() {
        return getActiveStorageType() == StorageType.SQLITE
                ? primaryRepository
                : secondaryRepository;
    }

    public int syncPrimaryToSecondary() {
        List<Note> primaryNotes = primaryRepository.getAllNotes();
        secondaryRepository.replaceAllNotes(primaryNotes);
        return primaryNotes.size();
    }

    public int syncSecondaryToPrimary() {
        List<Note> secondaryNotes = secondaryRepository.getAllNotes();
        primaryRepository.replaceAllNotes(secondaryNotes);
        return secondaryNotes.size();
    }

    /**
     * Merges notes from both primary and secondary storages by note ID, keeping the newer
     * version when both contain the same ID, and writes the merged result to both storages.
     */
    public int syncTwoWayMerge() {
        Map<String, Note> merged = new LinkedHashMap<>();
        mergeInto(merged, primaryRepository.getAllNotes());
        mergeInto(merged, secondaryRepository.getAllNotes());

        List<Note> mergedList = new ArrayList<>(merged.values());
        Collections.sort(mergedList, Comparator.comparingLong(Note::getUpdatedAt).reversed());

        primaryRepository.replaceAllNotes(mergedList);
        secondaryRepository.replaceAllNotes(mergedList);
        return mergedList.size();
    }

    private void mergeInto(Map<String, Note> merged, List<Note> notes) {
        for (Note note : notes) {
            Note existing = merged.get(note.getId());
            if (existing == null || note.getUpdatedAt() >= existing.getUpdatedAt()) {
                merged.put(note.getId(), note);
            }
        }
    }

    /**
     * Releases any underlying resources (such as SQLiteOpenHelper) held by the repositories.
     */
    public void close() {
        closeQuietly(primaryRepository);
        closeQuietly(secondaryRepository);
    }

    private void closeQuietly(NoteRepository repository) {
        if (repository instanceof AutoCloseable) {
            try {
                ((AutoCloseable) repository).close();
            } catch (Exception ignored) {
                // Best-effort cleanup
            }
        }
    }
}
