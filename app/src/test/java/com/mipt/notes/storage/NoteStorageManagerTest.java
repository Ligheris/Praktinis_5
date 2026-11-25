package com.mipt.notes.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.mipt.notes.model.Note;
import com.mipt.notes.storage.NoteStorageManager.StoragePreferenceStore;
import com.mipt.notes.storage.NoteStorageManager.StorageType;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NoteStorageManagerTest {

    private InMemoryNoteRepository primaryRepo;
    private InMemoryNoteRepository secondaryRepo;
    private NoteStorageManager manager;

    @Before
    public void setUp() {
        primaryRepo = new InMemoryNoteRepository();
        secondaryRepo = new InMemoryNoteRepository();
        StoragePreferenceStore prefStore = new StoragePreferenceStore() {
            private StorageType current = StorageType.SQLITE;

            @Override
            public StorageType getSelectedStorage() {
                return current;
            }

            @Override
            public void setSelectedStorage(StorageType storageType) {
                this.current = storageType;
            }
        };
        manager = new NoteStorageManager(primaryRepo, secondaryRepo, prefStore);
    }

    @Test
    public void activeRepository_switchesBetweenPrimaryAndSecondary() {
        assertEquals(StorageType.SQLITE, manager.getActiveStorageType());
        manager.getActiveRepository().saveNote(new Note("1", "SQLite Note", "Body", 100L));

        assertEquals(1, primaryRepo.getAllNotes().size());
        assertEquals(0, secondaryRepo.getAllNotes().size());

        manager.setActiveStorageType(StorageType.FILE);
        assertEquals(StorageType.FILE, manager.getActiveStorageType());
        manager.getActiveRepository().saveNote(new Note("2", "File Note", "Body", 200L));

        assertEquals(1, primaryRepo.getAllNotes().size());
        assertEquals(1, secondaryRepo.getAllNotes().size());
    }

    @Test
    public void syncPrimaryToSecondary_overwritesSecondaryWithPrimary() {
        primaryRepo.saveNote(new Note("1", "Primary", "Content", 100L));
        secondaryRepo.saveNote(new Note("2", "Old Secondary", "Content", 50L));

        int count = manager.syncPrimaryToSecondary();

        assertEquals(1, count);
        assertEquals(1, secondaryRepo.getAllNotes().size());
        assertEquals("Primary", secondaryRepo.getNoteById("1").getTitle());
        assertNull(secondaryRepo.getNoteById("2"));
    }

    @Test
    public void syncSecondaryToPrimary_overwritesPrimaryWithSecondary() {
        primaryRepo.saveNote(new Note("1", "Old Primary", "Content", 100L));
        secondaryRepo.saveNote(new Note("2", "Secondary", "Content", 200L));

        int count = manager.syncSecondaryToPrimary();

        assertEquals(1, count);
        assertEquals(1, primaryRepo.getAllNotes().size());
        assertEquals("Secondary", primaryRepo.getNoteById("2").getTitle());
        assertNull(primaryRepo.getNoteById("1"));
    }

    @Test
    public void syncTwoWayMerge_mergesUniqueNotesAndKeepsNewestOnConflict() {
        primaryRepo.saveNote(new Note("1", "Shared Older", "Old", 100L));
        primaryRepo.saveNote(new Note("2", "Only Primary", "P", 150L));

        secondaryRepo.saveNote(new Note("1", "Shared Newer", "New", 300L));
        secondaryRepo.saveNote(new Note("3", "Only Secondary", "S", 250L));

        int count = manager.syncTwoWayMerge();

        assertEquals(3, count);
        assertEquals(3, primaryRepo.getAllNotes().size());
        assertEquals(3, secondaryRepo.getAllNotes().size());
        assertEquals("Shared Newer", primaryRepo.getNoteById("1").getTitle());
        assertEquals("Shared Newer", secondaryRepo.getNoteById("1").getTitle());
        assertEquals("Only Primary", secondaryRepo.getNoteById("2").getTitle());
        assertEquals("Only Secondary", primaryRepo.getNoteById("3").getTitle());
    }

    private static class InMemoryNoteRepository implements NoteRepository {
        private final Map<String, Note> store = new LinkedHashMap<>();

        @Override
        public List<Note> getAllNotes() {
            List<Note> list = new ArrayList<>(store.values());
            Collections.sort(list, Comparator.comparingLong(Note::getUpdatedAt).reversed());
            return list;
        }

        @Override
        public Note getNoteById(String id) {
            return store.get(id);
        }

        @Override
        public void saveNote(Note note) {
            store.put(note.getId(), note);
        }

        @Override
        public void deleteNote(String id) {
            store.remove(id);
        }

        @Override
        public void replaceAllNotes(List<Note> notes) {
            store.clear();
            for (Note note : notes) {
                store.put(note.getId(), note);
            }
        }
    }
}
