package com.gentics.mesh.contentrag.common.model;

import java.util.Objects;

/**
 * Identitaet einer Document-Group: alle Chunks eines (Node, Branch, Sprache) bilden eine Gruppe.
 * Wird fuer Group-Delete-and-Replace und Idempotenz verwendet (Konzept K5/K6).
 *
 * @param nodeUuid UUID des Mesh-Nodes
 * @param branch   Branch-Name
 * @param language Sprachcode der Sprachversion
 */
public record DocumentGroupId(String nodeUuid, String branch, String language) {

    public DocumentGroupId {
        Objects.requireNonNull(nodeUuid, "nodeUuid");
        Objects.requireNonNull(branch, "branch");
        Objects.requireNonNull(language, "language");
    }

    /** Kanonische String-Form {@code nodeUuid:branch:language}, wie im Index-Feld {@code documentGroup}. */
    public String asKey() {
        return nodeUuid + ":" + branch + ":" + language;
    }
}
