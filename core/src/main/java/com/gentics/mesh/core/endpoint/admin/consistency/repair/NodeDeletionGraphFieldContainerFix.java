package com.gentics.mesh.core.endpoint.admin.consistency.repair;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.INITIAL;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import java.util.Iterator;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.dagger.MeshInternal;
import com.syncleus.ferma.FramedGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This fix will create the missing {@link Node} for {@link NodeGraphFieldContainer}'s which were affected by the deletion bug which was fixed in version
 * 0.18.3. Due to this bug the {@link Node} was deleted leaving the {@link NodeGraphFieldContainer} dangling in the graph.
 */
public class NodeDeletionGraphFieldContainerFix {

	private static final Logger log = LoggerFactory.getLogger(NodeDeletionGraphFieldContainerFix.class);

	public void repair(NodeGraphFieldContainer container) {

		BootstrapInitializer boot = MeshInternal.get().boot();
		// Pick the first project we find to fetch the initial branchUuid
		Project project = boot.meshRoot().getProjectRoot().findAll().iterator().next();
		String branchUuid = project.getInitialBranch().getUuid();

		SchemaContainer schemaContainer = container.getSchemaContainerVersion().getSchemaContainer();
		// 1. Find the initial version to check whether the whole version history is still intact
		NodeGraphFieldContainer initial = findInitial(container);

		if (initial == null) {
			// The container has no previous version or is not the initial version so we can just delete it.
			container.remove();
			return;
		}
		NodeGraphFieldContainer latest = findLatest(container);
		NodeGraphFieldContainer published = null;
		NodeGraphFieldContainer draft = null;
		if (latest.getVersion().getFullVersion().endsWith(".0")) {
			published = latest;
		} else {
			draft = latest;
		}

		if (published == null) {
			published = findPublished(latest);
		}
		if (draft == null) {
			draft = findDraft(latest);
		}

		log.info("Initial:" + initial.getUuid() + " version: " + initial.getVersion());
		if (draft != null) {
			log.info("Draft:" + draft.getUuid() + " version: " + draft.getVersion());
		} else {
			throw new RuntimeException("The draft version could not be found");
		}
		if (published != null) {
			log.info("Publish:" + published.getUuid() + " version: " + published.getVersion());
		} else {
			log.info("Published not found");
		}

		log.info("Schema container " + schemaContainer.getName());

		FramedGraph graph = container.getGraph();
		Node node = graph.addFramedVertex(NodeImpl.class);
		node.setProject(project);
		node.setCreated(project.getCreator());

		if (published != null) {
			GraphFieldContainerEdge edge = node.addFramedEdge(HAS_FIELD_CONTAINER, published, GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(published.getLanguage().getLanguageTag());
			edge.setBranchUuid(branchUuid);
			edge.setType(PUBLISHED);
		}

		GraphFieldContainerEdge edge = node.addFramedEdge(HAS_FIELD_CONTAINER, draft, GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(draft.getLanguage().getLanguageTag());
		edge.setBranchUuid(branchUuid);
		edge.setType(DRAFT);

		GraphFieldContainerEdge initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
		initialEdge.setLanguageTag(initial.getLanguage().getLanguageTag());
		initialEdge.setBranchUuid(branchUuid);
		initialEdge.setType(INITIAL);

		SearchQueueBatch batch = MeshInternal.get().searchQueue().create();
		BulkActionContext context = new BulkActionContext(batch);
		node.delete(context);

	}

	private NodeGraphFieldContainer findDraft(NodeGraphFieldContainer latest) {
		NodeGraphFieldContainer previous = latest.getPreviousVersion();
		while (previous != null) {
			if (!previous.getVersion().getFullVersion().equalsIgnoreCase(".0")) {
				return previous;
			}
			previous = previous.getPreviousVersion();
		}
		return null;	}

	/**
	 * Iterate over all versions and try to find the latest published version.
	 * 
	 * @param latest
	 * @return
	 */
	private NodeGraphFieldContainer findPublished(NodeGraphFieldContainer latest) {
		NodeGraphFieldContainer previous = latest.getPreviousVersion();
		while (previous != null) {
			if (previous.getVersion().getFullVersion().equalsIgnoreCase(".0")) {
				return previous;
			}
			previous = previous.getPreviousVersion();
		}
		return null;

	}

	private NodeGraphFieldContainer findLatest(NodeGraphFieldContainer container) {
		Iterator<? extends NodeGraphFieldContainer> it = container.getNextVersions().iterator();
		if (it.hasNext()) {
			NodeGraphFieldContainer next = it.next();
			if (it.hasNext()) {
				throw new RuntimeException("The version history has branches. The fix is currently unable to deal with version branches.");
			}
			return findLatest(next);
		} else {
			return container;
		}
	}

	private NodeGraphFieldContainer findInitial(NodeGraphFieldContainer container) {
		if (container.getVersion().getFullVersion().equalsIgnoreCase("0.1")) {
			return container;
		}
		NodeGraphFieldContainer initial = null;
		NodeGraphFieldContainer previous = container.getPreviousVersion();
		while (previous != null) {
			initial = previous;
			previous = previous.getPreviousVersion();
		}

		return initial;
	}

}
