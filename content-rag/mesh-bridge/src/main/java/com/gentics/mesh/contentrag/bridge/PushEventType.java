package com.gentics.mesh.contentrag.bridge;

/**
 * Art einer von Mesh gepushten Aenderung. In Anlehnung an die bestehende
 * Mesh-ES-Search-Integration (Konzept K1/K2).
 */
public enum PushEventType {
    /** Node erstellt/aktualisiert/publiziert -> Re-Index der Document-Group. */
    UPSERT,
    /** Node geloescht/depubliziert -> Document-Group entfernen. */
    DELETE,
    /** Rollen-/Permission-Aenderung -> readRoleUuids im Index aktualisieren (K2/K3). */
    PERMISSION_CHANGE
}
