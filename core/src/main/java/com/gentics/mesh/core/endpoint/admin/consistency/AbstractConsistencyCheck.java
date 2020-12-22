package com.gentics.mesh.core.endpoint.admin.consistency;

import java.util.Iterator;
import java.util.function.BiConsumer;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation of a consistency check which provides helper methods to track edges and vertices.
 */
public abstract class AbstractConsistencyCheck implements ConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(AbstractConsistencyCheck.class);
	private static final long BATCH_SIZE = 10000L;

	/**
	 * Loads the elements of the given type from the graph and processes them using the given action.
	 * 
	 * @param db
	 *            Database reference
	 * @param clazz
	 *            Type of elements to be loaded and processed
	 * @param action
	 *            Processing action to be invoked
	 * @param attemptRepair
	 *            Handle repair
	 * @param tx
	 *            Current transaction
	 */
	protected <T extends MeshVertex> ConsistencyCheckResult processForType(Database db, Class<T> clazz, BiConsumer<T, ConsistencyCheckResult> action,
		boolean attemptRepair, Tx tx) {
		log.info("Processing elements of type {" + clazz.getSimpleName() + "}");
		Iterator<? extends T> it = db.getVerticesForType(clazz);
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		long count = 0;
		while (it.hasNext()) {
			T element = it.next();
			action.accept(element, result);
			if (count != 0 && count % BATCH_SIZE == 0) {
				if (attemptRepair) {
					tx.getGraph().commit();
				}
				log.info("Processed {" + count + "} " + clazz.getSimpleName() + " elements.");
			}
			count++;
		}
		if (attemptRepair) {
			tx.getGraph().commit();
		}
		log.info("Processed a total of {" + count + "} " + clazz.getSimpleName() + " elements.");
		return result;
	}

	/**
	 * Check existence of an incoming edge.
	 *
	 * @param vertex
	 * @param edgeLabel
	 * @param clazz
	 * @param result
	 * @param severity
	 * @param edges
	 */
	protected <N extends MeshVertex> void checkIn(MeshVertex vertex, String edgeLabel, Class<N> clazz, ConsistencyCheckResult result,
		InconsistencySeverity severity, Edge... edges) {
		N ref = vertex.in(edgeLabel).has(clazz).nextOrDefaultExplicit(clazz, null);
		if (ref == null) {
			result.addInconsistency(String.format("%s: incoming edge %s from %s not found", vertex.getClass().getSimpleName(), edgeLabel, clazz
				.getSimpleName()), vertex.getUuid(), severity);
		} else if (edges.length > 0) {
			MeshVertex ref2 = vertex;
			for (Edge edge : edges) {
				ref2 = edge.follow(ref2);
				if (ref2 == null) {
					break;
				}
			}

			if (ref2 != null && !ref.equals(ref2)) {
				result.addInconsistency(String.format("%s: incoming edge %s from %s should be equal to %s but was %s", vertex.getClass()
					.getSimpleName(), edgeLabel, clazz.getSimpleName(), ref2.getUuid(), ref.getUuid()), vertex.getUuid(), severity);
			}
		}
	}

	/**
	 * Check existence of an outgoing edge.
	 *
	 * @param vertex
	 * @param edgeLabel
	 * @param clazz
	 * @param result
	 * @param severity
	 * @param edges
	 */
	protected <N extends MeshVertex> void checkOut(MeshVertex vertex, String edgeLabel, Class<N> clazz, ConsistencyCheckResult result,
		InconsistencySeverity severity, Edge... edges) {
		N ref = vertex.out(edgeLabel).has(clazz).nextOrDefaultExplicit(clazz, null);
		if (ref == null) {
			result.addInconsistency(String.format("%s: outgoing edge %s to %s not found", vertex.getClass().getSimpleName(), edgeLabel, clazz
				.getSimpleName()), vertex.getUuid(), severity);
		} else if (edges.length > 0) {
			MeshVertex ref2 = vertex;
			for (Edge edge : edges) {
				ref2 = edge.follow(ref2);
				if (ref2 == null) {
					break;
				}
			}

			if (ref2 != null && !ref.equals(ref2)) {
				result.addInconsistency(String.format("%s: outgoing edge %s to %s should be equal to %s but was %s", vertex.getClass()
					.getSimpleName(), edgeLabel, clazz.getSimpleName(), ref2.getUuid(), ref.getUuid()), vertex.getUuid(), severity);
			}
		}
	}

	/**
	 * Follow an incoming edge.
	 *
	 * @param label
	 * @param clazz
	 * @return
	 */
	protected Edge in(String label, Class<? extends MeshVertex> clazz) {
		return v -> v.in(label).has(clazz).nextOrDefault(clazz, null);
	}

	/**
	 * Follow an outgoing edge.
	 *
	 * @param label
	 * @param clazz
	 * @return
	 */
	protected Edge out(String label, Class<? extends MeshVertex> clazz) {
		return v -> v.out(label).has(clazz).nextOrDefault(clazz, null);
	}

	/**
	 * Interface for an edge follower.
	 */
	@FunctionalInterface
	public static interface Edge {

		/**
		 * Helper to follow the given vertex.
		 * 
		 * @param v
		 * @return
		 */
		MeshVertex follow(MeshVertex v);
	}
}
