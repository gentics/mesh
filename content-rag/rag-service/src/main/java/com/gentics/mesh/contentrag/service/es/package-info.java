/**
 * Lesender Elasticsearch-Zugriff des RAG-Service: Hybrid-Retrieval, Zaehlungen fuer den
 * Statusendpunkt und ein schmales Gateway um den Low-Level-RestClient.
 *
 * <p>Dieses Paket schreibt keine Chunk-Dokumente. Befuellt werden die RAG-Indizes
 * ausschliesslich von Mesh ueber dessen Standard-Sync; die Feld- und Indexnamen kommen aus dem
 * gemeinsamen Modul {@code index-contract}. Geschrieben werden hier nur die einmaligen
 * Cluster-Objekte (Inference-Endpunkt, Ingest-Pipeline, Index-Template) – siehe Paket
 * {@code setup}.
 */
package com.gentics.mesh.contentrag.service.es;
