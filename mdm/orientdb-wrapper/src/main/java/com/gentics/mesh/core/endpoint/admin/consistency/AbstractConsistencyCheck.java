package com.gentics.mesh.core.endpoint.admin.consistency;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.endpoint.admin.consistency.check.CommonConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;

/**
 * Abstract implementation of a consistency check which provides helper methods to track edges and vertices.
 */
public abstract class AbstractConsistencyCheck implements CommonConsistencyCheck {

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
