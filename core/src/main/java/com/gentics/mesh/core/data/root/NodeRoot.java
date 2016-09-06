package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.util.InvalidArgumentException;

/**
 * Aggregation node for nodes.
 */
public interface NodeRoot extends RootVertex<Node> {

	public static final String TYPE = "nodes";

	/**
	 * Find all nodes that are visible for the user.
	 * 
	 * @param requestUser
	 *            User that is used to check view permissions
	 * @param languageTags
	 * @param pagingInfo
	 *            Paging parameters
	 * @return Page with found nodes or an empty page
	 * @throws InvalidArgumentException
	 */
	PageImpl<? extends Node> findAll(MeshAuthUser requestUser, List<String> languageTags, PagingParameters pagingInfo) throws InvalidArgumentException;

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
	Node create(User user, SchemaContainerVersion container, Project project);

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
