/**
 * Mesh-seitiger Teil des RAG-Syncs: Feldauswahl (Allow-List je Schema), No-RAG-Konvention (K4)
 * und das Einsammeln der normalisierten Feldtexte.
 *
 * <p>Die Klassen dieses Pakets sind bewusst framework-frei und kennen weder die Mesh-Schema-API
 * noch Elasticsearch. Sie bilden nur die Entscheidung ab, welcher Inhalt das CMS Richtung
 * RAG-Index verlaesst – der Adapter auf {@code FieldSchema} und {@code HibNodeFieldContainer}
 * sowie die beiden Handler ({@code RagChunkIndexHandler}, {@code RagChunkEventHandler}) kommen
 * mit der Anbindung an den Mesh-Sync dazu.
 *
 * <p>Der Zuschnitt hat einen Grund: die Auswahlregeln sind der inhaltlich kniffligste Teil und
 * hier ohne laufendes Mesh testbar.
 */
package com.gentics.mesh.contentrag.meshsync;
