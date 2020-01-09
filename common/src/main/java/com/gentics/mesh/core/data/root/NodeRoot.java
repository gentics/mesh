package com.gentics.mesh.core.data.root;

import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.ContainerType;

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
	default Node create(User user, SchemaContainerVersion container, Project project) {
		return create(user, container, project, null);
	}

	Stream<? extends Node> findAllStream(InternalActionContext ac, ContainerType type);

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
	Node create(User user, SchemaContainerVersion container, Project project, String uuid);

}
