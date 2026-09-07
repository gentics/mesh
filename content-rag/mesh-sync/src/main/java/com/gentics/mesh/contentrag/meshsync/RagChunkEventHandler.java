package com.gentics.mesh.contentrag.meshsync;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.search.verticle.eventhandler.EventCauseHelper.isProjectDeleteCause;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Delta-Pfad des RAG-Index: uebersetzt Node-Events in Chunk-Upserts und Deletes.
 *
 * <p>Laeuft neben {@code NodeContentEventHandler} – {@code MainEventHandler} baut eine Multimap,
 * mehrere Handler duerfen dasselbe Event bearbeiten.
 *
 * <p>Drei bewusste Unterschiede zum Node-Handler:
 * <ol>
 * <li><strong>Eine Schema-Migration loest kein Delete aus.</strong> Der Node-Index traegt die
 *     {@code schemaVersionUuid} im Namen und muss die Dokumente der alten Version wegraeumen. Der
 *     RAG-Index tut das ausdruecklich nicht – ein Upsert in denselben Index genuegt. Genau dafuer
 *     ist der Indexname so gebaut.</li>
 * <li><strong>{@code NODE_MOVED} gehoert dazu.</strong> {@code path} und {@code sourceUrl} stehen
 *     im Chunk-Dokument und sind nach einem Verschieben falsch.</li>
 * <li><strong>Nur {@code PUBLISHED}.</strong> Events zu Entwurfsstaenden aendern am
 *     veroeffentlichten Inhalt nichts und werden verworfen; das spart im Redaktionsbetrieb den
 *     Grossteil der Ereignisse.</li>
 * </ol>
 */
@Singleton
public class RagChunkEventHandler implements EventHandler {

    private final MeshHelper helper;
    private final RagSyncOptions options;
    private final RagChunkWriter writer;

    /**
     * @param helper  Zugriff auf Datenbank und Request-Factory
     * @param options Betriebsparameter; {@code enabled=false} legt den Handler still
     * @param writer  gemeinsame Schreiblogik mit dem Voll-Sync
     */
    @Inject
    public RagChunkEventHandler(MeshHelper helper, RagSyncOptions options, RagChunkWriter writer) {
        this.helper = helper;
        this.options = options;
        this.writer = writer;
    }

    @Override
    public Collection<MeshEvent> handledEvents() {
        return Arrays.asList(
                NODE_CONTENT_CREATED, NODE_UPDATED, NODE_PUBLISHED, NODE_MOVED,
                NODE_CONTENT_DELETED, NODE_UNPUBLISHED, NODE_DELETED);
    }

    @Override
    public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
        if (!options.enabled()) {
            return Flowable.empty();
        }
        return Flowable.defer(() -> {
            NodeMeshEventModel message = requireType(NodeMeshEventModel.class, messageEvent.message);
            if (isProjectDeleteCause(message)) {
                // Der Index des Projekts wird als Ganzes verworfen; Einzel-Deletes waeren verschwendet.
                return Flowable.empty();
            }
            return switch (messageEvent.event) {
                case NODE_CONTENT_CREATED, NODE_UPDATED, NODE_PUBLISHED -> upsertLanguage(message);
                case NODE_MOVED -> upsertNode(message);
                case NODE_CONTENT_DELETED, NODE_UNPUBLISHED -> purgeLanguage(message);
                case NODE_DELETED -> purgeNode(message);
                default -> Flowable.empty();
            };
        });
    }

    /** Aktualisiert die Chunks einer Sprachversion. */
    private Flowable<SearchRequest> upsertLanguage(NodeMeshEventModel message) {
        if (!isPublished(message)) {
            return Flowable.empty();
        }
        return Flowable.fromIterable(helper.getDb().tx(tx -> {
            HibProject project = tx.projectDao().findByUuid(message.getProject().getUuid());
            if (project == null) {
                return List.<SearchRequest>of();
            }
            HibBranch branch = tx.branchDao().findByUuid(project, message.getBranchUuid());
            HibNode node = tx.nodeDao().findByUuid(project, message.getUuid());
            if (branch == null || node == null) {
                return List.<SearchRequest>of();
            }
            HibNodeFieldContainer container = tx.contentDao()
                    .getFieldContainer(node, message.getLanguageTag(), branch.getUuid(), PUBLISHED);
            if (container == null) {
                return List.of(writer.purgeLanguage(project.getUuid(), branch.getUuid(), node.getUuid(),
                        message.getLanguageTag()));
            }
            return writer.upsert(container, project, branch, PUBLISHED);
        }));
    }

    /**
     * Aktualisiert alle Sprachversionen eines Nodes – nach einem Verschieben stimmen Pfad und
     * Quellen-URL in jedem Chunk nicht mehr.
     */
    private Flowable<SearchRequest> upsertNode(NodeMeshEventModel message) {
        return Flowable.fromIterable(helper.getDb().tx(tx -> {
            HibProject project = tx.projectDao().findByUuid(message.getProject().getUuid());
            if (project == null) {
                return List.<SearchRequest>of();
            }
            HibBranch branch = tx.branchDao().findByUuid(project, message.getBranchUuid());
            HibNode node = tx.nodeDao().findByUuid(project, message.getUuid());
            if (branch == null || node == null) {
                return List.<SearchRequest>of();
            }
            List<SearchRequest> requests = new ArrayList<>();
            for (HibNodeFieldContainer container : tx.contentDao()
                    .getFieldContainers(node, branch.getUuid(), PUBLISHED)) {
                requests.addAll(writer.upsert(container, project, branch, PUBLISHED));
            }
            return requests;
        }));
    }

    private Flowable<SearchRequest> purgeLanguage(NodeMeshEventModel message) {
        if (!isPublished(message)) {
            return Flowable.empty();
        }
        return Flowable.just(writer.purgeLanguage(message.getProject().getUuid(), message.getBranchUuid(),
                message.getUuid(), message.getLanguageTag()));
    }

    private Flowable<SearchRequest> purgeNode(NodeMeshEventModel message) {
        return Flowable.just(writer.purgeNode(message.getProject().getUuid(), message.getBranchUuid(),
                message.getUuid()));
    }

    /**
     * Ereignisse zu Entwurfsstaenden werden verworfen: der RAG-Index kennt im MVP nur
     * {@code PUBLISHED}. Ein Event ohne Typangabe wird mitgenommen.
     */
    private static boolean isPublished(NodeMeshEventModel message) {
        return message.getType() == null || message.getType() == PUBLISHED;
    }
}
