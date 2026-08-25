package com.gentics.mesh.contentrag.common.spi;

import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.SourceContent;

import java.util.List;

/**
 * Strukturbasiertes Chunking (Konzept 5.4) entlang Ueberschriften und Absaetzen, mit harten
 * Grenzen an Feld-, Sprach- und Node-Grenzen. Erzeugt deterministische Chunk-IDs (K5).
 *
 * <p>Wird auf der <strong>Mesh-Seite</strong> aufgerufen, waehrend der Standard-Sync das
 * RAG-Chunk-Dokument aufbaut. Die Referenzimplementierung liegt framework-frei im Modul
 * {@code chunking}, damit sie ohne Spring in den Mesh-Prozess eingebunden werden kann.
 * Die zurueckgegebenen Chunks tragen keinen Vektor – den ergaenzt Elasticsearch beim Indexieren.
 */
public interface Chunker {

    /**
     * Zerlegt den normalisierten Inhalt eines Nodes in indexierbare Chunks.
     *
     * @param content normalisierter Quellinhalt einer Document-Group
     * @return Chunks in stabiler Reihenfolge; leer, wenn kein indexierbarer Text vorhanden ist
     */
    List<Chunk> chunk(SourceContent content);
}
