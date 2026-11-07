/**
 * Einmalige Elasticsearch-Administration beim Start: Inference-Endpunkt, Ingest-Pipeline und
 * Index-Template. Diese Objekte haengen am Embedding-Modell und gehoeren damit auf die
 * RAG-Seite – Mesh legt seine Indizes weiterhin unveraendert an, das Template greift von selbst.
 */
package com.gentics.mesh.contentrag.service.setup;
