package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class MeshNodeService extends AbstractMeshService {

	public static MeshNodeService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static MeshNodeService getNodeService() {
		return instance;
	}

	//private static ForkJoinPool pool = new ForkJoinPool(8);

	public Page<? extends MeshNode> findAll(MeshShiroUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = requestUser.getPermTraversal(READ_PERM).has(MeshNode.class).mark().out(ASSIGNED_TO_PROJECT)
				.has("name", projectName).back();
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getPermTraversal(READ_PERM).has(MeshNode.class).mark().out(ASSIGNED_TO_PROJECT)
				.has("name", projectName).back();
		Page<? extends MeshNode> nodePage = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, MeshNode.class);
		return nodePage;
	}

	public void createLink(MeshNode from, MeshNode to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

	public List<? extends MeshNode> findAllNodes() {
		return fg.v().has(MeshNode.class).toListExplicit(MeshNode.class);
	}

	public MeshNode create() {
		return fg.addFramedVertex(MeshNode.class);
	}

	public MeshNode findByUUID(String uuid) {
		return fg.v().has("uuid", uuid).nextOrDefault(MeshNode.class, null);
	}

	public void delete(MeshNode node) {
		node.getVertex().remove();
	}

}
