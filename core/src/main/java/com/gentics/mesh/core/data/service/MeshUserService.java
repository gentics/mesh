package com.gentics.mesh.core.data.service;

import java.util.List;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.MeshAuthUser;
import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.model.impl.MeshUserImpl;
import com.gentics.mesh.paging.PagingInfo;

@Component
public class MeshUserService extends AbstractMeshGraphService<MeshUser> {

	private static final Logger log = LoggerFactory.getLogger(MeshUserService.class);

	public static MeshUserService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static MeshUserService getUserService() {
		return instance;
	}

	/**
	 * Find all users that are visible for the given user.
	 */
	public Page<MeshUser> findAllVisible(MeshAuthUser requestUser, PagingInfo pagingInfo) {
		// String userUuid = session.getPrincipal().getString("uuid");
		// return findAll(userUuid, new MeshPageRequest(pagingInfo));
		return null;
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) where requestUser.uuid = {0} and perm.`permissions-read` = true return user ORDER BY user.username",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) where requestUser.uuid = {0} and perm.`permissions-read` = true return count(user)")
	}

	public MeshUser findByUsername(String username) {
		return fg.v().has(MeshUserImpl.USERNAME_KEY, username).nextOrDefault(MeshUserImpl.class, null);
	}

	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return fg.v().has(MeshUserImpl.USERNAME_KEY, username).nextOrDefaultExplicit(MeshAuthUserImpl.class, null);
	}
	
	@Override
	public List<? extends MeshUser> findAll() {
		return fg.v().has(MeshUserImpl.class).toListExplicit(MeshUserImpl.class);
	}

	@Override
	public MeshUser findByUUID(String uuid) {
		return findByUUID(uuid, MeshUserImpl.class);
	}

}
