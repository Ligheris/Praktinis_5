package com.mipt.notes.validation;

/**
 * Validates note title and content before persistence.
 */
public final class NoteValidator {

    public enum ValidationResult {
        VALID,
        EMPTY_TITLE,
        EMPTY_CONTENT,
        EMPTY_BOTH
    }

    private NoteValidator() {
    }

    public static ValidationResult validate(String title, String content) {
        boolean titleEmpty = isBlank(title);
        boolean contentEmpty = isBlank(content);

        if (titleEmpty && contentEmpty) {
            return ValidationResult.EMPTY_BOTH;
        }
        if (titleEmpty) {
            return ValidationResult.EMPTY_TITLE;
        }
        if (contentEmpty) {
            return ValidationResult.EMPTY_CONTENT;
        }
        return ValidationResult.VALID;
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
