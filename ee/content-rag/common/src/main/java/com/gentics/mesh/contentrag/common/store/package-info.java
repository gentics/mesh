/**
 * Abstraktionen fuer die Persistenz der Betriebsdaten des RAG-Service: Konfiguration und
 * Audit (Konzept 6.1). Default-Backend Elasticsearch, optional JPA.
 *
 * <p>Der frueher hier gefuehrte Sync-State entfaellt: Mesh kennt den Sync-Zustand seiner
 * Indizes selbst, eine zweite Buchfuehrung darueber waere nur eine weitere Quelle fuer
 * Abweichungen.
 */
package com.gentics.mesh.contentrag.common.store;
