package com.mipt.notes.storage;

import com.mipt.notes.model.Note;

import java.util.List;

/**
 * Contract for note persistence backends (SQLite, internal file, etc.).
 */
public interface NoteRepository {

    List<Note> getAllNotes();

    Note getNoteById(String id);

    void saveNote(Note note);

    void deleteNote(String id);

    void replaceAllNotes(List<Note> notes);
}
