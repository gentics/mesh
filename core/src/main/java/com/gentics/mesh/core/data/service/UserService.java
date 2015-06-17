package com.gentics.mesh.core.data.service;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.root.UserRoot;
import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.TraversalHelper;
import com.tinkerpop.blueprints.Vertex;

@Component
public class UserService extends AbstractMeshService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	/**
	 * Find all users that are visible for the given user.
	 * 
	 * @param requestUser
	 * @param pagingInfo
	 * @return
	 */
	public Page<MeshUser> findAllVisible(MeshShiroUser requestUser, PagingInfo pagingInfo) {
		// String userUuid = session.getPrincipal().getString("uuid");
		// return findAll(userUuid, new MeshPageRequest(pagingInfo));
		return null;
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) where requestUser.uuid = {0} and perm.`permissions-read` = true return user ORDER BY user.username",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) where requestUser.uuid = {0} and perm.`permissions-read` = true return count(user)")
	}

	public MeshUser findByUsername(String username) {
		return TraversalHelper.nextExplicitOrNull(fg.v().has("username", username).has(MeshUser.class), MeshUser.class);
	}

	//TODO reduce calls to this method
	public MeshUser findUser(RoutingContext rc) {
		String userUuid = rc.user().principal().getString("uuid");
		return findByUUID(userUuid);
	}

	//
	//	User findByPrincipalId(String principalId) {
	//		// @Query("MATCH (u:_User) WHERE u.username + '%' + u.emailAddress + '%' +  u.passwordHash = {0} return u")
	//		return null;
	//	}

	public UserRoot findRoot() {
		return TraversalHelper.nextExplicitOrNull(fg.v().has(UserRoot.class), UserRoot.class);
	}

	public MeshUser create(String username) {
		MeshUser user = fg.addFramedVertex(MeshUser.class);
		user.setUsername(username);
		return user;
	}

	public void setPassword(MeshUser user, String password) {
		user.setPasswordHash(springConfiguration.passwordEncoder().encode(password));
	}

	public UserRoot createRoot() {
		UserRoot root = fg.addFramedVertex(UserRoot.class);
		return root;
	}

	public MeshUser findOne(Long id) {
		Vertex vertex = fg.getVertex(id);
		if (vertex != null) {
			return fg.frameElement(vertex, MeshUser.class);
		}
		return null;
	}

	public MeshUser findByUUID(String uuid) {
		return TraversalHelper.nextExplicitOrNull(fg.v().has("uuid", uuid).has(MeshUser.class), MeshUser.class);
	}

}
