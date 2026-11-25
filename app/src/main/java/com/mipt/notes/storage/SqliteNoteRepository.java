package com.mipt.notes.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mipt.notes.model.Note;

import java.util.ArrayList;
import java.util.List;

/**
 * Primary note repository backed by an Android SQLite database.
 */
public class SqliteNoteRepository extends SQLiteOpenHelper implements NoteRepository {

    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NOTES = "notes";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_CONTENT = "content";
    private static final String COL_UPDATED_AT = "updated_at";

    public SqliteNoteRepository(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NOTES + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_TITLE + " TEXT NOT NULL, "
                + COL_CONTENT + " TEXT NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    @Override
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String orderBy = COL_UPDATED_AT + " DESC";
        try (Cursor cursor = db.query(TABLE_NOTES, null, null, null, null, null, orderBy)) {
            while (cursor.moveToNext()) {
                notes.add(readNoteFromCursor(cursor));
            }
        }
        return notes;
    }

    @Override
    public Note getNoteById(String id) {
        if (id == null) {
            return null;
        }
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_NOTES,
                null,
                COL_ID + " = ?",
                new String[]{id},
                null,
                null,
                null)) {
            if (cursor.moveToFirst()) {
                return readNoteFromCursor(cursor);
            }
        }
        return null;
    }

    @Override
    public void saveNote(Note note) {
        if (note == null) {
            throw new IllegalArgumentException("Note must not be null");
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = toContentValues(note);
        db.insertWithOnConflict(TABLE_NOTES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void deleteNote(String id) {
        if (id == null) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NOTES, COL_ID + " = ?", new String[]{id});
    }

    @Override
    public void replaceAllNotes(List<Note> notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_NOTES, null, null);
            if (notes != null) {
                for (Note note : notes) {
                    db.insertWithOnConflict(
                            TABLE_NOTES,
                            null,
                            toContentValues(note),
                            SQLiteDatabase.CONFLICT_REPLACE
                    );
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toContentValues(Note note) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, note.getId());
        values.put(COL_TITLE, note.getTitle());
        values.put(COL_CONTENT, note.getContent());
        values.put(COL_UPDATED_AT, note.getUpdatedAt());
        return values;
    }

    private Note readNoteFromCursor(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
        long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT));
        return new Note(id, title, content, updatedAt);
    }
}
