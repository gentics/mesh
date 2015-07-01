package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class NodeService extends AbstractMeshGraphService<Node> {

	public static NodeService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static NodeService getNodeService() {
		return instance;
	}

	public Page<? extends Node> findAll(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(NodeImpl.class).mark()
				.out(ASSIGNED_TO_PROJECT).has("name", projectName).back();
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(NodeImpl.class).mark()
				.out(ASSIGNED_TO_PROJECT).has("name", projectName).back();
		Page<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
		return nodePage;
	}

	public void createLink(Node from, Node to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

	@Override
	public List<? extends Node> findAll() {
		return fg.v().has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public Node findByUUID(String uuid) {
		return findByUUID(uuid, NodeImpl.class);
	}

}
