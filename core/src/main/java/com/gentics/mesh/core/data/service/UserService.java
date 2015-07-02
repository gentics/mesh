package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class UserService extends AbstractMeshGraphService<User> {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	public static UserService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static UserService getUserService() {
		return instance;
	}

	/**
	 * Find all users that are visible for the given user.
	 * 
	 * @throws InvalidArgumentException
	 */
	public Page<? extends User> findAllVisible(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().out(HAS_GROUP).out(HAS_ROLE).out(READ_PERM.label()).has(UserImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().out(HAS_GROUP).out(HAS_ROLE).out(READ_PERM.label()).has(UserImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, UserImpl.class);
	}

	public User findByUsername(String username) {
		return fg.v().has(UserImpl.class).has(UserImpl.USERNAME_KEY, username).nextOrDefault(UserImpl.class, null);
	}

	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return fg.v().has(UserImpl.class).has(UserImpl.USERNAME_KEY, username).nextOrDefaultExplicit(MeshAuthUserImpl.class, null);
	}

	@Override
	public List<? extends User> findAll() {
		return fg.v().has(UserImpl.class).toListExplicit(UserImpl.class);
	}

	@Override
	public User findByUUID(String uuid) {
		return findByUUID(uuid, UserImpl.class);
	}

}
