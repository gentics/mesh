package com.gentics.mesh.contentrag.service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Kleine Hash-Helfer. Wird an zwei Stellen gebraucht, an denen ein Klartextwert nicht liegen
 * bleiben soll: als Cache-Schluessel fuer Tokens und als Pseudonym im Audit-Log.
 */
final class Hashes {

    private Hashes() {
    }

    /** Vollstaendiger SHA-256 als Hex. */
    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nicht verfuegbar", e);
        }
    }

    /** Auf 16 Hex-Zeichen gekuerzter SHA-256; ausreichend eindeutig als Pseudonym. */
    static String shortSha256(String value) {
        return sha256(value).substring(0, 16);
    }
}
