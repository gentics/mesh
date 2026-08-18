package com.gentics.mesh.contentrag.common.spi;

import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.SourceContent;

import java.util.List;

/**
 * Strukturbasiertes semantisches Chunking (Konzept 5.4) entlang Ueberschriften/Absaetzen/Listen.
 * Harte Grenzen an Sprach- und Node-Grenzen. Erzeugt deterministische Chunk-IDs (K5).
 * Die zurueckgegebenen Chunks tragen noch kein Embedding (siehe {@link EmbeddingProvider}).
 */
public interface Chunker {

    List<Chunk> chunk(SourceContent content);
}
