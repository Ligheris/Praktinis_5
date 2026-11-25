package com.mipt.notes.validation;

import static org.junit.Assert.assertEquals;

import com.mipt.notes.validation.NoteValidator.ValidationResult;

import org.junit.Test;

public class NoteValidatorTest {

    @Test
    public void validate_returnsValid_whenTitleAndContentAreNonBlank() {
        assertEquals(ValidationResult.VALID, NoteValidator.validate("Shopping", "Milk and bread"));
    }

    @Test
    public void validate_returnsEmptyTitle_whenTitleIsBlank() {
        assertEquals(ValidationResult.EMPTY_TITLE, NoteValidator.validate("   ", "Some content"));
        assertEquals(ValidationResult.EMPTY_TITLE, NoteValidator.validate(null, "Some content"));
    }

    @Test
    public void validate_returnsEmptyContent_whenContentIsBlank() {
        assertEquals(ValidationResult.EMPTY_CONTENT, NoteValidator.validate("Title", "  \n "));
        assertEquals(ValidationResult.EMPTY_CONTENT, NoteValidator.validate("Title", null));
    }

    @Test
    public void validate_returnsEmptyBoth_whenBothAreBlank() {
        assertEquals(ValidationResult.EMPTY_BOTH, NoteValidator.validate("", "   "));
        assertEquals(ValidationResult.EMPTY_BOTH, NoteValidator.validate(null, null));
    }
}
