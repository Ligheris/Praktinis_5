package com.mipt.notes.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user note with a unique ID, title, content, and last-modified timestamp.
 */
public class Note {
    private final String id;
    private String title;
    private String content;
    private long updatedAt;

    public Note(String id, String title, String content, long updatedAt) {
        this.id = (id == null || id.trim().isEmpty()) ? UUID.randomUUID().toString() : id;
        this.title = title;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public static Note createNew(String title, String content) {
        return new Note(UUID.randomUUID().toString(), title, content, System.currentTimeMillis());
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note)) return false;
        Note note = (Note) o;
        return updatedAt == note.updatedAt
                && Objects.equals(id, note.id)
                && Objects.equals(title, note.title)
                && Objects.equals(content, note.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, updatedAt);
    }
}
