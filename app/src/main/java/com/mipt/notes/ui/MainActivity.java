package com.mipt.notes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mipt.notes.R;
import com.mipt.notes.model.Note;
import com.mipt.notes.storage.NoteStorageManager;
import com.mipt.notes.storage.NoteStorageManager.StorageType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays the list of notes from the active storage backend, allows switching between
 * SQLite (primary) and File (secondary), and provides synchronization options.
 */
public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteActionListener {

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private NoteStorageManager storageManager;
    private NoteAdapter adapter;
    private View emptyStateView;
    private RecyclerView recyclerView;
    private MaterialButtonToggleGroup storageToggleGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storageManager = new NoteStorageManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        emptyStateView = findViewById(R.id.empty_state_view);
        recyclerView = findViewById(R.id.recycler_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(this);
        recyclerView.setAdapter(adapter);

        setupStorageToggle();

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_note);
        fabAdd.setOnClickListener(v -> openEditor(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storageManager != null) {
            ioExecutor.execute(storageManager::close);
        }
        ioExecutor.shutdown();
    }

    private void setupStorageToggle() {
        storageToggleGroup = findViewById(R.id.storage_toggle_group);
        StorageType activeType = storageManager.getActiveStorageType();
        storageToggleGroup.check(activeType == StorageType.SQLITE
                ? R.id.btn_storage_sqlite
                : R.id.btn_storage_file);

        storageToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            StorageType newType = (checkedId == R.id.btn_storage_sqlite)
                    ? StorageType.SQLITE
                    : StorageType.FILE;
            if (newType != storageManager.getActiveStorageType()) {
                storageManager.setActiveStorageType(newType);
                refreshNotes();
            }
        });
    }

    private void refreshNotes() {
        ioExecutor.execute(() -> {
            try {
                List<Note> notes = storageManager.getActiveRepository().getAllNotes();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    adapter.submitList(notes);
                    boolean isEmpty = notes.isEmpty();
                    emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                });
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, R.string.error_storage_operation, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void openEditor(String noteId) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        if (noteId != null) {
            intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, noteId);
        }
        startActivity(intent);
    }

    @Override
    public void onNoteClick(Note note) {
        openEditor(note.getId());
    }

    @Override
    public void onNoteDeleteClick(Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(getString(R.string.delete_dialog_message, note.getTitle()))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> ioExecutor.execute(() -> {
                    try {
                        storageManager.getActiveRepository().deleteNote(note.getId());
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                                refreshNotes();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sync) {
            showSyncDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSyncDialog() {
        String[] options = new String[]{
                getString(R.string.sync_primary_to_secondary),
                getString(R.string.sync_secondary_to_primary),
                getString(R.string.sync_two_way_merge)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.sync_dialog_title)
                .setItems(options, (dialog, which) -> ioExecutor.execute(() -> {
                    try {
                        int count;
                        if (which == 0) {
                            count = storageManager.syncPrimaryToSecondary();
                        } else if (which == 1) {
                            count = storageManager.syncSecondaryToPrimary();
                        } else {
                            count = storageManager.syncTwoWayMerge();
                        }
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(
                                        this,
                                        getString(R.string.sync_result_message, count),
                                        Toast.LENGTH_SHORT
                                ).show();
                                refreshNotes();
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
