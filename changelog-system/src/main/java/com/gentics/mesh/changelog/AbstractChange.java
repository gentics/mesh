package com.gentics.mesh.changelog;

import java.util.Iterator;

import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractChange implements Change {

	protected static final Logger log = LoggerFactory.getLogger(AbstractChange.class);

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
		Vertex meshRoot = graph.getVertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, "com.gentics.mesh.core.data.root.impl.MeshRootImpl")
				.iterator().next();
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

}
