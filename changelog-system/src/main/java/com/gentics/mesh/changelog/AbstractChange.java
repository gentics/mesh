package com.gentics.mesh.changelog;

import java.util.Iterator;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.util.StreamUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Basic implementation of a changelog change. All change implementations should extend this class.
 */
public abstract class AbstractChange implements Change {

	protected static final Logger log = LoggerFactory.getLogger(AbstractChange.class);

	private Graph graph;

	private GraphDatabase db;

	private long duration;

	@Override
	public void apply() {
		applyOutsideTx();
		Graph graph = db.rawTx();
		setGraph(graph);
		try {
			applyInTx();
			graph.tx().commit();
		} catch (Throwable e) {
			log.error("Invoking rollback due to error", e);
			graph.tx().rollback();
			throw e;
		} finally {
			graph.tx().close();
		}
	}

	/**
	 * Run the given action multiple times. Abort if the action returns false.
	 * 
	 * @param action
	 */
	protected void runBatchAction(BooleanSupplier action) {
		do {
			log.info("Running batch");
		} while (applyBatchActionInTx(action));
	}

	/**
	 * Run the given action in a transaction and return its value.
	 * 
	 * @param action
	 * @return
	 */
	protected boolean applyBatchActionInTx(BooleanSupplier action) {
		Graph graph = getDb().rawTx();
		setGraph(graph);
		try {
			boolean b = action.getAsBoolean();
			getGraph().tx().commit();
			return b;
		} finally {
			graph.tx().close();
		}
	}

	/**
	 * You may override this method to apply code outside of the transaction (e.g. for schema changes)
	 */
	public void applyOutsideTx() {
		// Noop
	}

	/**
	 * You may override this method to apply code within a transaction.
	 */
	public void applyInTx() {

	}

	@Override
	public abstract String getUuid();

	@Override
	public boolean isApplied() {
		Graph graph = db.rawTx();
		setGraph(graph);
		ChangelogRootWrapper changelogRoot = changelogRoot();
		boolean hasChange = changelogRoot.hasChange(getUuid());
		graph.tx().close();
		setGraph(null);
		return hasChange;
	}

	/**
	 * Return the changelog root wrapper. The wrapper wraps the graph element which stored changelog information.
	 * 
	 * @return
	 */
	private ChangelogRootWrapper changelogRoot() {
		Vertex meshRoot = getMeshRootVertex();
		if (meshRoot == null) {
			throw new RuntimeException("Could not find mesh root node. The change can't be applied without the mesh root vertex.");
		}
		Iterator<Vertex> it = meshRoot.vertices(Direction.OUT, ChangelogRootWrapper.HAS_CHANGELOG_ROOT);
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

	public Vertex getMeshRootVertex() {
		return MeshGraphHelper.getMeshRootVertex(getGraph());
	}

	@Override
	public void markAsComplete() {
		Graph graph = db.rawTx();
		setGraph(graph);
		changelogRoot().add(this);
		setGraph(null);
		graph.tx().commit();
		graph.tx().close();
	}

	@Override
	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	@Override
	public Graph getGraph() {
		return graph;
	}

	/**
	 * Create a random UUID string which does not include dashes.
	 * 
	 * @return
	 */
	public String randomUUID() {
		final UUID uuid = UUID.randomUUID();
		String randomUuid = (digits(uuid.getMostSignificantBits() >> 32, 8) + digits(uuid.getMostSignificantBits() >> 16, 4) + digits(uuid
			.getMostSignificantBits(), 4) + digits(uuid.getLeastSignificantBits() >> 48, 4) + digits(uuid.getLeastSignificantBits(), 12));
		return randomUuid;
	}

	protected void fail(String msg) {
		throw new RuntimeException(msg);
	}

	/**
	 * Returns val represented by the specified number of hex digits.
	 * 
	 * @param val
	 * @param digits
	 * @return
	 */
	private static String digits(long val, int digits) {
		long hi = 1L << (digits * 4);
		return Long.toHexString(hi | (val & (hi - 1))).substring(1);
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

	@Override
	public boolean requiresReindex() {
		return false;
	}

	@Override
	public GraphDatabase getDb() {
		return db;
	}

	@Override
	public void setDb(GraphDatabase db) {
		this.db = db;
	}

	/**
	 * Iterates over some items, committing the transaction after some iterations.
	 * @param iterable
	 * @param commitInterval
	 * @param consumer
	 * @param <T>
	 */
	protected <T> void iterateWithCommit(Iterable<T> iterable, int commitInterval, Consumer<T> consumer) {
		int count = 0;
		for (T item : iterable) {
			consumer.accept(item);
			count++;
			if (count % commitInterval == 0) {
				log.info("Migrated {" + count + "} contents");
				getGraph().tx().commit();
			}
		}
	}

	/**
	 * Iterates over some items, committing the transaction after 1000 iterations.
	 * @param iterable
	 * @param consumer
	 */
	protected <T> void iterateWithCommit(Iterable<T> iterable, Consumer<T> consumer) {
		iterateWithCommit(iterable, 1000, consumer);
	}

	/**
	 * Replaces the first edge with <code>direction</code> and <code>label</code> from all vertices of type <code>vertexClass</code>
	 * with a property with key <code>uuidPropertyKey</code> containing the uuid of the connected vertex.
	 *
	 * @param vertexClass
	 * @param direction
	 * @param label
	 * @param uuidPropertyKey
	 */
	protected void replaceSingleEdge(String vertexClass, Direction direction, String label, String uuidPropertyKey) {
		iterateWithCommit(StreamUtil.toIterable(getGraph().vertices("@class", vertexClass)), vertex ->
			replaceSingleEdge(vertex, direction, label, uuidPropertyKey));
	}

	/**
	 * Replaces the first edge with <code>direction</code> and <code>label</code> from the given vertex
	 * with a property with key <code>uuidPropertyKey</code> containing the uuid of the connected vertex.
	 *
	 * @param vertex
	 * @param direction
	 * @param label
	 * @param uuidPropertyKey
	 */
	protected void replaceSingleEdge(Vertex vertex, Direction direction, String label, String uuidPropertyKey) {
		Iterator<Edge> edges = vertex.edges(direction, label);
		if (!edges.hasNext()) {
			log.warn(String.format("Expected vertex with uuid %s to have %s edge %s, but none was found", vertex.property("uuid").orElse(null), direction, label));
			return;
		}
		Edge edge = edges.next();
		Vertex opvertex;
		switch (direction.opposite()) {
		case IN:
			opvertex = edge.inVertex();
		case OUT:
			opvertex = edge.outVertex();
		default:
			throw new IllegalStateException("Unsupported case: " + direction.opposite());
		}
		String uuid = opvertex.<String>property("uuid").orElse(null);
		vertex.property(uuidPropertyKey, uuid);
		edge.remove();
	}

	private void debug(Element element) {
		System.out.println("---");
		for (Property<?> p : StreamUtil.<Property<?>>toIterable(element.properties())) {
			System.out.println(p.key() + " : " + p.orElse(null));
		}
		System.out.println("---");
	}

}
