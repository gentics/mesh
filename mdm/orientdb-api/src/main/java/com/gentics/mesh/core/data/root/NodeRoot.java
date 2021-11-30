package com.gentics.mesh.core.data.root;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Aggregation node for nodes.
 */
public interface NodeRoot extends RootVertex<Node> {

	public static final String TYPE = "nodes";

	// Move these to DAO
	/**
	 * Create a new node.
	 * 
	 * @param user
	 *            User that is used to set creator and editor references
	 * @param container
	 *            Schema version that should be used when creating the node
	 * @param project
	 *            Project to which the node should be assigned to
	 * @return Created node
	 */
	@Deprecated
	default Node create(HibUser user, HibSchemaVersion container, HibProject project) {
		return create(user, container, project, null);
	}

	/**
	 * Create a new node.
	 * 
	 * @param user
	 *            User that is used to set creator and editor references
	 * @param container
	 *            Schema version that should be used when creating the node
	 * @param project
	 *            Project to which the node should be assigned to
	 * @param uuid
	 *            Optional uuid
	 * @return Created node
	 */
	@Deprecated
	Node create(HibUser user, HibSchemaVersion container, HibProject project, String uuid);

	@Deprecated
	Node create(InternalActionContext ac, EventQueueBatch batch, String uuid);

}
