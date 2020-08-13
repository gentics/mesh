package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;

/**
 * Aggregation node for nodes.
 */
public interface NodeRoot extends RootVertex<Node> {

	public static final String TYPE = "nodes";

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
	default Node create(HibUser user, SchemaVersion container, HibProject project) {
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
	Node create(HibUser user, SchemaVersion container, HibProject project, String uuid);

}
