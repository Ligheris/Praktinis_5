package com.mipt.notes.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mipt.notes.R;
import com.mipt.notes.model.Note;
import com.mipt.notes.storage.NoteRepository;
import com.mipt.notes.storage.NoteStorageManager;
import com.mipt.notes.validation.NoteValidator;
import com.mipt.notes.validation.NoteValidator.ValidationResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for creating, viewing, editing, and deleting a single note in the active storage.
 */
public class NoteEditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "extra_note_id";

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private NoteStorageManager storageManager;
    private NoteRepository activeRepository;
    private Note existingNote;

    private TextInputLayout titleInputLayout;
    private TextInputLayout contentInputLayout;
    private TextInputEditText titleEditText;
    private TextInputEditText contentEditText;
    private MaterialButton saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        storageManager = new NoteStorageManager(this);
        activeRepository = storageManager.getActiveRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        titleInputLayout = findViewById(R.id.input_layout_title);
        contentInputLayout = findViewById(R.id.input_layout_content);
        titleEditText = findViewById(R.id.edit_note_title);
        contentEditText = findViewById(R.id.edit_note_content);
        saveButton = findViewById(R.id.btn_save_note);

        String noteId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        if (noteId != null) {
            setTitle(R.string.editor_title_edit);
            saveButton.setEnabled(false);
            loadExistingNote(noteId);
        } else {
            setTitle(R.string.editor_title_new);
        }

        saveButton.setOnClickListener(v -> attemptSave());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storageManager != null) {
            ioExecutor.execute(storageManager::close);
        }
        ioExecutor.shutdown();
    }

    private void loadExistingNote(String noteId) {
        ioExecutor.execute(() -> {
            try {
                Note loaded = activeRepository.getNoteById(noteId);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (loaded == null) {
                        Toast.makeText(this, R.string.note_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    existingNote = loaded;
                    titleEditText.setText(existingNote.getTitle());
                    contentEditText.setText(existingNote.getContent());
                    saveButton.setEnabled(true);
                    invalidateOptionsMenu();
                });
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, R.string.error_storage_operation, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });
    }

    private void attemptSave() {
        String rawTitle = titleEditText.getText() != null ? titleEditText.getText().toString() : "";
        String rawContent = contentEditText.getText() != null ? contentEditText.getText().toString() : "";

        titleInputLayout.setError(null);
        contentInputLayout.setError(null);

        ValidationResult validation = NoteValidator.validate(rawTitle, rawContent);
        if (validation != ValidationResult.VALID) {
            if (validation == ValidationResult.EMPTY_TITLE || validation == ValidationResult.EMPTY_BOTH) {
                titleInputLayout.setError(getString(R.string.error_empty_title));
            }
            if (validation == ValidationResult.EMPTY_CONTENT || validation == ValidationResult.EMPTY_BOTH) {
                contentInputLayout.setError(getString(R.string.error_empty_content));
            }
            return;
        }

        String trimmedTitle = rawTitle.trim();
        String trimmedContent = rawContent.trim();

        Note noteToSave;
        if (existingNote != null) {
            existingNote.setTitle(trimmedTitle);
            existingNote.setContent(trimmedContent);
            existingNote.setUpdatedAt(System.currentTimeMillis());
            noteToSave = existingNote;
        } else {
            noteToSave = Note.createNew(trimmedTitle, trimmedContent);
        }

        saveButton.setEnabled(false);
        ioExecutor.execute(() -> {
            try {
                activeRepository.saveNote(noteToSave);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        saveButton.setEnabled(true);
                        Toast.makeText(this, R.string.error_storage_operation, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (existingNote != null) {
            getMenuInflater().inflate(R.menu.menu_note_editor, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete && existingNote != null) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        String noteIdToDelete = existingNote.getId();
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(getString(R.string.delete_dialog_message, existingNote.getTitle()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> ioExecutor.execute(() -> {
                    try {
                        activeRepository.deleteNote(noteIdToDelete);
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        });
                    } catch (RuntimeException e) {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(this, R.string.error_storage_operation, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
