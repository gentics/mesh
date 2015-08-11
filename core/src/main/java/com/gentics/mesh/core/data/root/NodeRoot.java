package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.util.InvalidArgumentException;

public interface NodeRoot extends RootVertex<Node> {

	public static final String TYPE = "nodes";

	/**
	 * Find all nodes that are visible for the user.
	 * 
	 * @param requestUser
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 * @throws InvalidArgumentException
	 */
	Page<? extends Node> findAll(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo) throws InvalidArgumentException;

	/**
	 * Create a new node.
	 * 
	 * @param user
	 * @param container
	 * @param project
	 * @return
	 */
	Node create(User user, SchemaContainer container, Project project);

	/**
	 * Add the node to the aggregation node.
	 * 
	 * @param node
	 */
	void addNode(Node node);

	/**
	 * Remove the node from the aggregation node.
	 * 
	 * @param node
	 */
	void removeNode(Node node);

}
