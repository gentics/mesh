package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.common.RestModel;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract class for mesh core vertices that includes methods which are commonly used when transforming the vertices into REST POJO's.
 * 
 * @param <T>
 *            Rest model representation of the core vertex
 * @param <R>
 *            Type of the core vertex which is used to determine type of chained vertices
 */
public abstract class AbstractMeshCoreVertex<T extends RestModel> extends MeshVertexImpl
	implements MeshCoreVertex<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractMeshCoreVertex.class);

	/**
	 * Compare both values in order to determine whether the graph value should be updated.
	 * 
	 * @param restValue
	 *            Rest model string value
	 * @param graphValue
	 *            Graph string value
	 * @return true if restValue is not null and the restValue is not equal to the graph value. Otherwise false.
	 * @deprecated This method was moved to AbstractDaoWrapper
	 */
	@Deprecated
	protected <T> boolean shouldUpdate(T restValue, T graphValue) {
		return restValue != null && !restValue.equals(graphValue);
	}

	@Override
	public String mapGraphQlFieldName(String gqlName) {
		switch (gqlName) {
		case "edited": return "last_edited_timestamp";
		case "created":	return "creation_timestamp";
		default: 
			return super.mapGraphQlFieldName(gqlName);
		}
	}
}
