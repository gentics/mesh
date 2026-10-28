package com.gentics.mesh.contentrag.service.security;

/**
 * Loest das mitgelieferte Mesh-Token in den zugehoerigen Benutzer samt Rollen auf.
 *
 * <p>Die Trennung als eigenes Interface ist Absicht: die Rollenherkunft ist die
 * sicherheitskritische Stelle des gesamten Systems, und sie soll an genau einem Ort austauschbar
 * sein — etwa wenn spaeter ein OIDC-Token mit Rollen-Claims dazukommt (O5).
 */
public interface PrincipalResolver {

    /**
     * @param token Mesh-Token des Aufrufers; {@code null} fuer eine Anfrage ohne Anmeldung
     * @return der aufgeloeste Benutzer samt Rollen
     * @throws InvalidTokenException  wenn Mesh das Token ablehnt
     * @throws MeshUnavailableException wenn Mesh nicht erreichbar ist
     */
    MeshPrincipal resolve(String token);
}
