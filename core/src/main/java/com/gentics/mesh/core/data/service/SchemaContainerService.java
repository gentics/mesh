package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;

import java.awt.print.Pageable;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshUser;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.impl.MeshUserImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.paging.PagingInfo;

@Component
public class SchemaContainerService extends AbstractMeshGraphService<SchemaContainer> {

	public static SchemaContainerService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static SchemaContainerService getSchemaService() {
		return instance;
	}

	public SchemaContainer findByName(String projectName, String name) {
		if (StringUtils.isEmpty(projectName) || StringUtils.isEmpty(name)) {
			throw new NullPointerException("name or project name null");
		}
		return fg.v().has("name", name).has(SchemaContainerImpl.class).mark().out(ASSIGNED_TO_PROJECT).has("name", projectName).back()
				.nextOrDefault(SchemaContainerImpl.class, null);
	}

	public List<? extends SchemaContainer> findAll(String projectName) {
		// @Query("MATCH (n:ObjectSchema)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
		return null;
	}

	public Page<SchemaContainer> findAll(MeshUserImpl requestUser, Pageable pageable) {
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return schema ORDER BY schema.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return count(schema)")
		return null;
	}

	public Page<SchemaContainer> findAllVisible(MeshUser requestUser, PagingInfo pagingInfo) {
		// return findAll(requestUser, new MeshPageRequest(pagingInfo));
		return null;
	}

	@Override
	public List<? extends SchemaContainer> findAll() {
		return fg.v().has(SchemaContainerImpl.class).toListExplicit(SchemaContainerImpl.class);
	}

	public SchemaContainer findByName(String name) {
		return findByName(name, SchemaContainerImpl.class);
	}

	public SchemaContainer findByUUID(String projectName, String uuid) {
		// TODO check for projectName
		return fg.v().has("uuid", uuid).has(SchemaContainerImpl.class).nextOrDefault(SchemaContainerImpl.class, null);
	}

	@Override
	public SchemaContainer findByUUID(String uuid) {
		return findByUUID(uuid, SchemaContainerImpl.class);
	}

}
