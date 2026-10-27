/**
 * Strukturbasiertes Chunking als framework-freie Bibliothek.
 *
 * <p>Laeuft auf der Mesh-Seite, waehrend der Standard-Sync das RAG-Chunk-Dokument aufbaut.
 * Bewusst ohne Spring-, Elasticsearch- oder HTTP-Abhaengigkeit: was in den Mesh-Prozess
 * eingebunden wird, soll dort nichts weiter mitbringen.
 */
package com.gentics.mesh.contentrag.chunking;
