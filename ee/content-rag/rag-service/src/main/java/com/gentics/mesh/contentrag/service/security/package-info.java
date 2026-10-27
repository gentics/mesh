/**
 * Auflösung des Anfragenden gegen Gentics Mesh.
 *
 * <p>Kern der Berechtigungsstrategie (Konzept 10): der Aufrufer schickt sein Mesh-Token, der
 * Service fragt damit bei Mesh nach Benutzer und Rollen, und ausschliesslich diese Rollen gehen
 * in den Filter des Retrievals. Der Aufrufer kann seine Rollen also nicht selbst behaupten —
 * genau das war vorher moeglich, solange {@code roleUuids} im Request-Body stand.
 *
 * <p>Mesh bleibt damit die einzige Autoritaet fuer Berechtigungen, so wie fuer die
 * Volltextsuche auch. Der Service haelt keine eigene Benutzer- oder Rollenverwaltung und
 * benutzt insbesondere <strong>kein</strong> Dienst-Token fuer Leseentscheidungen.
 */
package com.gentics.mesh.contentrag.service.security;
