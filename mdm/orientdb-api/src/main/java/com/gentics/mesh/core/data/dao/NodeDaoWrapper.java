package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;

/**
 * DAO to access {@link HibNode}.
 */
public interface NodeDaoWrapper extends NodeDao {

	/**
	 * Set the graph field container to be the (only) published for the given branch.
	 *
	 * @param ac
	 * @param container
	 * @param branchUuid
	 */
	void setPublished(HibNode node, InternalActionContext ac, NodeGraphFieldContainer container, String branchUuid);
}
