package com.gentics.mesh.contentrag.service.security;

import java.util.List;

/**
 * Der Anfragende, wie Mesh ihn sieht. Einzige Quelle der Rollen, nach denen das Retrieval
 * filtert – der Aufrufer kann sie nicht selbst behaupten.
 *
 * @param userUuid  UUID des Mesh-Benutzers
 * @param username  Anmeldename (nur fuer Diagnose)
 * @param roleUuids Rollen-UUIDs des Benutzers; identische Semantik wie {@code _roleUuids}
 *                  im Index
 * @param anonymous ob die Anfrage ohne Token kam und damit als anonymer Mesh-Benutzer laeuft
 */
public record MeshPrincipal(
        String userUuid,
        String username,
        List<String> roleUuids,
        boolean anonymous
) {
    /** Name des Request-Attributs, unter dem der Filter den Principal ablegt. */
    public static final String ATTRIBUTE = "meshPrincipal";

    /** Defensivkopie der Rollenliste. */
    public MeshPrincipal {
        roleUuids = roleUuids == null ? List.of() : List.copyOf(roleUuids);
    }

    /**
     * Pseudonymisierte Nutzerreferenz fuer das Audit-Log (Datensparsamkeit, Konzept 13):
     * die ersten 16 Hex-Zeichen des SHA-256 ueber die Benutzer-UUID.
     *
     * @return stabiles Pseudonym oder {@code null} bei unbekanntem Benutzer
     */
    public String auditRef() {
        return userUuid == null ? null : Hashes.shortSha256(userUuid);
    }
}
