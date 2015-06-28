package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.MeshAuthUser;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.node.impl.MeshNodeImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class MeshNodeService extends AbstractMeshGraphService<MeshNode> {

	public static MeshNodeService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static MeshNodeService getNodeService() {
		return instance;
	}

	public Page<? extends MeshNode> findAll(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(MeshNodeImpl.class).mark()
				.out(ASSIGNED_TO_PROJECT).has("name", projectName).back();
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(MeshNodeImpl.class).mark()
				.out(ASSIGNED_TO_PROJECT).has("name", projectName).back();
		Page<? extends MeshNode> nodePage = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, MeshNodeImpl.class);
		return nodePage;
	}

	public void createLink(MeshNode from, MeshNode to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

	@Override
	public List<? extends MeshNode> findAll() {
		return fg.v().has(MeshNodeImpl.class).toListExplicit(MeshNodeImpl.class);
	}

	@Override
	public MeshNode findByUUID(String uuid) {
		return findByUUID(uuid, MeshNodeImpl.class);
	}

}
