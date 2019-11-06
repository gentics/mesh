package com.gentics.mesh.changelog;

import java.util.Iterator;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Basic implementation of a changelog change. All change implementations should extend this class.
 */
public abstract class AbstractChange implements Change {

	protected static final Logger log = LoggerFactory.getLogger(AbstractChange.class);

	private TransactionalGraph graph;

	private Database db;

	private long duration;

	@Override
	public void apply() {
		applyOutsideTx();
		TransactionalGraph graph = db.rawTx();
		setGraph(graph);
		try {
			applyInTx();
		} catch (Throwable e) {
			log.error("Invoking rollback due to error", e);
			graph.rollback();
			throw e;
		} finally {
			graph.shutdown();
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
		TransactionalGraph graph = getDb().rawTx();
		setGraph(graph);
		try {
			boolean b = action.getAsBoolean();
			getGraph().commit();
			return b;
		} finally {
			graph.shutdown();
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
		TransactionalGraph graph = db.rawTx();
		setGraph(graph);
		ChangelogRootWrapper changelogRoot = changelogRoot();
		boolean hasChange = changelogRoot.hasChange(getUuid());
		graph.shutdown();
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

	public Vertex getMeshRootVertex() {
		return MeshGraphHelper.getMeshRootVertex(getGraph());
	}

	@Override
	public void markAsComplete() {
		TransactionalGraph graph = db.rawTx();
		setGraph(graph);
		changelogRoot().add(this);
		setGraph(null);
		graph.commit();
		graph.shutdown();
	}

	@Override
	public void setGraph(TransactionalGraph graph) {
		this.graph = graph;
	}

	@Override
	public TransactionalGraph getGraph() {
		return graph;
	}

	public static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator();

	/**
	 * Create a random UUID string which does not include dashes.
	 * 
	 * @return
	 */
	public String randomUUID() {
		final UUID uuid = UUID_GENERATOR.generate();
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
	public Database getDb() {
		return db;
	}

	@Override
	public void setDb(Database db) {
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
				getGraph().commit();
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

	public void debug(Element element) {
		System.out.println("---");
		for (String key : element.getPropertyKeys()) {
			System.out.println(key + " : " + element.getProperty(key));
		}
		System.out.println("---");
	}

}
