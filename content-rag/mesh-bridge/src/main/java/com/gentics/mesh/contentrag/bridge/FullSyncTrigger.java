package com.gentics.mesh.contentrag.bridge;

/**
 * Anstoss eines Voll-/Initial-Syncs. Statt eines eigenen Pull-Reconcilers wird Mesh'
 * vorhandener Full-Sync ({@code POST /api/v2/search/sync}) und der periodische
 * Index-Korrektheits-Check wiederverwendet (Konzept K6/7). Diese Schnittstelle kapselt
 * nur das Durchreichen des Triggers; die eigentliche Sync-Mechanik liegt bei Mesh.
 *
 * <p>PLATZHALTER: Die konkrete Anbindung haengt an O8 (Hook-Variante) und O9
 * (welche Teile der bestehenden ES-Integration 1:1 uebernehmbar sind).
 */
public interface FullSyncTrigger {

    /** Stoesst einen Full-Sync fuer ein Projekt an (alle Branches gemaess Konfiguration). */
    void triggerFullSync(String projectUuid);
}
