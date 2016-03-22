package com.gentics.mesh.changelog;

import java.util.Iterator;

import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Basic implementation of a changelog change. All change implementations should extend this class.
 */
public abstract class AbstractChange implements Change {

	protected static final Logger log = LoggerFactory.getLogger(AbstractChange.class);

	private static final String MESH_ROOT_TYPE = "com.gentics.mesh.core.data.root.impl.MeshRootImpl";

	private TransactionalGraph graph;

	private long duration;

	public abstract void apply();

	@Override
	public String getUuid() {
		return getClass().getName().replaceAll("Change_", "");
	}

	@Override
	public boolean isApplied() {
		ChangelogRootWrapper changelogRoot = changelogRoot();
		return changelogRoot.hasChange(getUuid());
	}

	/**
	 * Return the changelog root wrapper. The wrapper wraps the graph element which stored changelog information.
	 * 
	 * @return
	 */
	private ChangelogRootWrapper changelogRoot() {
		Vertex meshRoot = getMeshRootVertex();
		Iterator<Vertex> it = meshRoot.getVertices(Direction.OUT, ChangelogRootWrapper.HAS_CHANGELOG_ROOT).iterator();
		Vertex changelogRoot = null;
		if (it.hasNext()) {
			changelogRoot = it.next();
		}

		// Create the change if it could not be found.
		if (changelogRoot == null) {
			log.debug("The changelog root could not be found. Creating it...");
			changelogRoot = graph.addVertex(ChangelogRootWrapper.class);
			meshRoot.addEdge(ChangelogRootWrapper.HAS_CHANGELOG_ROOT, changelogRoot);
		}
		return new ChangelogRootWrapper(graph, changelogRoot);
	}

	/**
	 * Add a new search queue batch and entry which will trigger a full reindex of all elements for the given type.
	 * 
	 * @param elementType
	 */
	protected void addFullReindexEntry(String elementType) {
		Vertex meshRootVertex = getMeshRootVertex();
		Vertex searchQueueRoot = meshRootVertex.getVertices(Direction.OUT, "HAS_SEARCH_QUEUE_ROOT").iterator().next();

		// 1. Add batch
		Vertex batch = getGraph().addVertex(null);
		batch.setProperty("batch_id", UUIDUtil.randomUUID());
		searchQueueRoot.addEdge("HAS_BATCH", batch);

		// 2. Add entry to batch 
		Vertex entry = getGraph().addVertex(null);
		entry.setProperty("element_type", "node");
		entry.setProperty("element_action", "reindex_all");
		batch.addEdge("HAS_ITEM", entry);
	}

	@Override
	public void markAsComplete() {
		changelogRoot().add(this);
	}

	@Override
	public void setGraph(TransactionalGraph graph) {
		this.graph = graph;
	}

	@Override
	public TransactionalGraph getGraph() {
		return graph;
	}

	/**
	 * Return the mesh root vertex.
	 * 
	 * @return
	 */
	protected Vertex getMeshRootVertex() {
		Vertex meshRoot = graph.getVertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, MESH_ROOT_TYPE).iterator().next();
		return meshRoot;
	}

	@Override
	public long getDuration() {
		return this.duration;
	}

	@Override
	public void setDuration(long timeMs) {
		this.duration = timeMs;
	}

	@Override
	public boolean validate() {
		return true;
	}

}
