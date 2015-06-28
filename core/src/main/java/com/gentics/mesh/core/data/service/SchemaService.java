package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;

import java.awt.print.Pageable;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.Schema;
import com.gentics.mesh.core.data.model.impl.MeshUserImpl;
import com.gentics.mesh.core.data.model.impl.SchemaImpl;
import com.gentics.mesh.paging.PagingInfo;

@Component
public class SchemaService extends AbstractMeshGraphService<Schema> {

	public static SchemaService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static SchemaService getSchemaService() {
		return instance;
	}

	public Schema findByName(String projectName, String name) {
		if (StringUtils.isEmpty(projectName) || StringUtils.isEmpty(name)) {
			throw new NullPointerException("name or project name null");
		}
		return fg.v().has("name", name).has(SchemaImpl.class).mark().out(ASSIGNED_TO_PROJECT).has("name", projectName).back()
				.nextOrDefault(SchemaImpl.class, null);
	}

	public List<? extends Schema> findAll(String projectName) {
		// @Query("MATCH (n:ObjectSchema)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
		return null;
	}

	public Page<Schema> findAll(MeshUserImpl requestUser, Pageable pageable) {
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return schema ORDER BY schema.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return count(schema)")
		return null;
	}

	public Page<Schema> findAllVisible(MeshUser requestUser, PagingInfo pagingInfo) {
		// return findAll(requestUser, new MeshPageRequest(pagingInfo));
		return null;
	}

	@Override
	public List<? extends Schema> findAll() {
		return fg.v().has(SchemaImpl.class).toListExplicit(SchemaImpl.class);
	}

	public Schema findByName(String name) {
		return findByName(name, SchemaImpl.class);
	}

	public Schema findByUUID(String projectName, String uuid) {
		// TODO check for projectName
		return fg.v().has("uuid", uuid).has(SchemaImpl.class).nextOrDefault(SchemaImpl.class, null);
	}

	@Override
	public Schema findByUUID(String uuid) {
		return findByUUID(uuid, SchemaImpl.class);
	}

}
