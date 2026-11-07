package io.gentics.mesh.mcp.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BearerTokenValidatorTest {

    @Test
    void accepts_correctBearer() {
        AuthValidator v = new BearerTokenValidator("s3cret");
        assertTrue(v.isAuthorized("Bearer s3cret"));
    }

    @Test
    void accepts_caseInsensitivePrefix() {
        AuthValidator v = new BearerTokenValidator("s3cret");
        assertTrue(v.isAuthorized("bearer s3cret"));
        assertTrue(v.isAuthorized("BEARER s3cret"));
    }

    @Test
    void rejects_null() {
        AuthValidator v = new BearerTokenValidator("s3cret");
        assertFalse(v.isAuthorized(null));
    }

    @Test
    void rejects_missingPrefix() {
        AuthValidator v = new BearerTokenValidator("s3cret");
        assertFalse(v.isAuthorized("s3cret"));
    }

    @Test
    void rejects_wrongToken() {
        AuthValidator v = new BearerTokenValidator("s3cret");
        assertFalse(v.isAuthorized("Bearer wrong"));
    }

    @Test
    void rejects_emptyToken() {
        AuthValidator v = new BearerTokenValidator("s3cret");
        assertFalse(v.isAuthorized("Bearer "));
    }

    @Test
    void constructor_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new BearerTokenValidator(""));
        assertThrows(IllegalArgumentException.class, () -> new BearerTokenValidator("   "));
        assertThrows(IllegalArgumentException.class, () -> new BearerTokenValidator(null));
    }

    @Test
    void allowAll_acceptsEverything() {
        assertTrue(AuthValidator.ALLOW_ALL.isAuthorized(null));
        assertTrue(AuthValidator.ALLOW_ALL.isAuthorized("anything"));
    }
}
