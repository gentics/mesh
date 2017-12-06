package com.gentics.mesh.core.verticle.admin.consistency;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * A consistency check must identify and log database inconsistencies.
 */
public interface ConsistencyCheck {
	/**
	 * Invoke the consistency check and update the given response with found inconsistencies.
	 * @param db database
	 * @param response
	 */
	void invoke(Database db, ConsistencyCheckResponse response);

	/**
	 * Check existence of an incoming edge
	 * @param vertex
	 * @param edgeLabel
	 * @param clazz
	 * @param response
	 * @param severity
	 * @param edges
	 */
	default <N extends MeshVertex> void checkIn(MeshVertex vertex, String edgeLabel, Class<N> clazz, ConsistencyCheckResponse response,
			InconsistencySeverity severity, Edge...edges) {
		N ref = vertex.in(edgeLabel).has(clazz).nextOrDefaultExplicit(clazz, null);
		if (ref == null) {
			response.addInconsistency(
					String.format("%s: incoming edge %s from %s not found", vertex.getClass().getSimpleName(), edgeLabel, clazz.getSimpleName()),
					vertex.getUuid(), severity);
		} else if (edges.length > 0) {
			MeshVertex ref2 = vertex;
			for (Edge edge : edges) {
				ref2 = edge.follow(ref2);
				if (ref2 == null) {
					break;
				}
			}

			if (ref2 != null && !ref.equals(ref2)) {
				response.addInconsistency(String.format("%s: incoming edge %s from %s should be equal to %s but was %s", vertex.getClass().getSimpleName(), edgeLabel,
						clazz.getSimpleName(), ref2.getUuid(), ref.getUuid()), vertex.getUuid(), severity);
			}
		}
	}

	/**
	 * Check existence of an outgoing edge
	 * @param vertex
	 * @param edgeLabel
	 * @param clazz
	 * @param response
	 * @param severity
	 * @param 
	 */
	default <N extends MeshVertex> void checkOut(MeshVertex vertex, String edgeLabel, Class<N> clazz, ConsistencyCheckResponse response,
			InconsistencySeverity severity, Edge... edges) {
		N ref = vertex.out(edgeLabel).has(clazz).nextOrDefaultExplicit(clazz, null);
		if (ref == null) {
			response.addInconsistency(String.format("%s: outgoing edge %s to %s not found", vertex.getClass().getSimpleName(), edgeLabel, clazz.getSimpleName()),
					vertex.getUuid(), severity);
		} else if (edges.length > 0) {
			MeshVertex ref2 = vertex;
			for (Edge edge : edges) {
				ref2 = edge.follow(ref2);
				if (ref2 == null) {
					break;
				}
			}

			if (ref2 != null && !ref.equals(ref2)) {
				response.addInconsistency(String.format("%s: outgoing edge %s to %s should be equal to %s but was %s", vertex.getClass().getSimpleName(), edgeLabel,
						clazz.getSimpleName(), ref2.getUuid(), ref.getUuid()), vertex.getUuid(), severity);
			}
		}
	}

	/**
	 * Follow an incoming edge
	 * @param label
	 * @param clazz
	 * @return
	 */
	default Edge in(String label, Class<? extends MeshVertex> clazz) {
		return v -> v.in(label).has(clazz).nextOrDefault(clazz, null);
	}

	/**
	 * Follow an outgoing edge
	 * @param label
	 * @param clazz
	 * @return
	 */
	default Edge out(String label, Class<? extends MeshVertex> clazz) {
		return v -> v.out(label).has(clazz).nextOrDefault(clazz, null);
	}

	/**
	 * Interface for an edge follower
	 */
	@FunctionalInterface
	public static interface Edge {
		MeshVertex follow(MeshVertex v);
	}
}
