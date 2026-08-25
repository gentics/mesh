/**
 * Gemeinsamer Kontrakt zwischen der schreibenden Mesh-Seite und dem lesenden RAG-Service.
 *
 * <p>Die Aufgabenteilung: Mesh schreibt ueber seinen Standard-Sync-Mechanismus in eigene
 * RAG-Chunk-Indizes ({@link com.gentics.mesh.contentrag.index.RagIndexNaming}), Elasticsearch
 * erzeugt beim Indexieren den Vektor
 * ({@link com.gentics.mesh.contentrag.index.RagInferenceSetup}), und der RAG-Service liest
 * ausschliesslich. Keine Seite kennt die interne Logik der anderen – gemeinsam ist nur, was
 * in diesem Paket steht.
 *
 * <p>Warum eigene Indizes statt zusaetzlicher Felder in den Node-Indizes der Volltextsuche:
 * <ul>
 *   <li><strong>Granularitaet.</strong> Ein Node-Dokument hat die ID
 *       {@code nodeUuid-language}; RAG braucht Chunk-Ebene, also mehrere Dokumente je Node.</li>
 *   <li><strong>Lebenszyklus.</strong> Node-Indizes tragen die Schemaversion im Namen, jede
 *       Migration erzeugt neue Indizes. Der Vektorbestand muesste dann jedes Mal komplett neu
 *       entstehen.</li>
 *   <li><strong>Betrieb.</strong> Vektoren dominieren die Indexgroesse. In eigenen Indizes
 *       bleiben Heap-Bedarf, Merge-Verhalten, Snapshot-Groesse und Recovery-Zeit der
 *       Volltextsuche unveraendert – auch fuer Installationen ohne RAG.</li>
 * </ul>
 *
 * <p>Was dennoch identisch bleibt: der Schreibweg (Mesh-Standard-Sync inklusive Full-Sync und
 * periodischem Korrektheits-Check), die Elasticsearch-Instanz und die Permission-Semantik ueber
 * {@link com.gentics.mesh.contentrag.index.RagChunkFields#ROLE_UUIDS}.
 */
package com.gentics.mesh.contentrag.index;
