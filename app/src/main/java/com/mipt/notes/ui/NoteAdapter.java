package com.mipt.notes.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mipt.notes.R;
import com.mipt.notes.model.Note;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView adapter for displaying notes with click-to-edit and quick delete actions.
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteActionListener {
        void onNoteClick(Note note);

        void onNoteDeleteClick(Note note);
    }

    private final List<Note> notes = new ArrayList<>();
    private final OnNoteActionListener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT
    );

    public NoteAdapter(OnNoteActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Note> newNotes) {
        notes.clear();
        if (newNotes != null) {
            notes.addAll(newNotes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        Context context = holder.itemView.getContext();

        holder.titleText.setText(note.getTitle());
        holder.contentText.setText(note.getContent());
        String formattedDate = dateFormat.format(new Date(note.getUpdatedAt()));
        holder.updatedText.setText(context.getString(R.string.note_updated_format, formattedDate));

        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note));
        holder.deleteButton.setOnClickListener(v -> listener.onNoteDeleteClick(note));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView contentText;
        final TextView updatedText;
        final ImageButton deleteButton;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.text_note_title);
            contentText = itemView.findViewById(R.id.text_note_content);
            updatedText = itemView.findViewById(R.id.text_note_updated);
            deleteButton = itemView.findViewById(R.id.btn_delete_note);
        }
    }
}
