package com.gentics.mesh.core.data.dao;

import java.util.Iterator;

import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibContent;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * DAO for {@link HibContent} operations.
 */
public interface ContentDaoWrapper extends ContentDao {

	/**
	 * Return an iterator over the edges for the given type and branch.
	 *
	 * @param type
	 * @param branchUuid
	 * @return
	 */
	Iterator<GraphFieldContainerEdge> getContainerEdge(HibNodeFieldContainer content, ContainerType type, String branchUuid);
}
