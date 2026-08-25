/**
 * Kerninterfaces (Service Provider Interfaces) der RAG-Pipeline: Chunker (Mesh-Seite),
 * LLM-Provider und Retriever (Query-Seite). Indexer, Content-Reader und Embedding-Provider
 * entfallen, seit Mesh ueber seinen Standard-Sync schreibt und Elasticsearch vektorisiert.
 */
package com.gentics.mesh.contentrag.common.spi;
