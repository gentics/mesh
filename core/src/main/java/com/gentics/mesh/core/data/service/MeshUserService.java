package com.gentics.mesh.core.data.service;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.root.UserRoot;
import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.TraversalHelper;
import com.tinkerpop.blueprints.Vertex;

@Component
public class MeshUserService extends AbstractMeshService {

	public static MeshUserService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static MeshUserService getUserService() {
		return instance;
	}

	private static final Logger log = LoggerFactory.getLogger(MeshUserService.class);

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
		return fg.v().has(MeshUser.USERNAME_KEY, username).nextOrDefault(MeshUser.class, null);
	}

	//
	// User findByPrincipalId(String principalId) {
	// // @Query("MATCH (u:_User) WHERE u.username + '%' + u.emailAddress + '%' +  u.passwordHash = {0} return u")
	// return null;
	// }

	public UserRoot findRoot() {
		return fg.v().nextOrDefault(UserRoot.class, null);
	}

	public MeshUser create(String username) {
		MeshUser user = fg.addFramedVertex(MeshUser.class);
		user.setUsername(username);
		UserRoot root = findRoot();
		root.addUser(user);
		return user;
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
		return fg.v().has("uuid", uuid).nextOrDefault(MeshUser.class, null);
	}

}
