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
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.root.impl.SchemaRootImpl;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyTypeImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.tinkerpop.blueprints.Vertex;

@Component
public class SchemaService extends AbstractMeshService {

	public static SchemaService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static SchemaService getSchemaService() {
		return instance;
	}

	public Schema findByUUID(String projectName, String uuid) {
		// TODO check for projectName
		return fg.v().has("uuid", uuid).has(SchemaImpl.class).nextOrDefault(SchemaImpl.class, null);
	}

	public Schema findByName(String projectName, String name) {
		if (StringUtils.isEmpty(projectName) || StringUtils.isEmpty(name)) {
			throw new NullPointerException("name or project name null");
		}
		return fg.v().has("name", name).has(SchemaImpl.class).mark().out(ASSIGNED_TO_PROJECT).has("name", projectName).back()
				.nextOrDefault(SchemaImpl.class, null);
	}

	public void deleteByUUID(String uuid) {
	}

	public Page<Schema> findAllVisible(MeshUser requestUser, PagingInfo pagingInfo) {
		// return findAll(requestUser, new MeshPageRequest(pagingInfo));
		return null;
	}

	public BasicPropertyTypeImpl getPropertyTypeSchema(String typeKey) {
		// if (StringUtils.isEmpty(typeKey)) {
		// return null;
		// }
		// for (BasicPropertyTypeSchema propertyTypeSchema : propertyTypeSchemas) {
		// if (propertyTypeSchema.getKey().equals(typeKey)) {
		// return propertyTypeSchema;
		// }
		// }
		// return null;
		return null;
	}

	/**
	 * Delete the object schema and all assigned relationships like permissions and creator information. Also delete the connected PropertyTypeSchemas.
	 */
	public void deleteByName(String projectName, String schemaName) {
		// @Query("MATCH (project:Project)<-[:ASSIGNED_TO_PROJECT]-(n:ObjectSchema {name: {1}}) OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema) OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema)-[rp]-() OPTIONAL MATCH (n)-[r2]-() WHERE project.name = {0} DELETE n,r,p,r2,rp")
	}

	/**
	 * Delete the object schema and all assigned relationships like permissions and creator information. Also delete the connected PropertyTypeSchemas.
	 */
	public void deleteByUuid(String uuid) {
		// TODO check for schema class
		fg.v().has("uuid", uuid).remove();
		// @Query("MATCH (n:ObjectSchema {uuid: {0}}) OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema) OPTIONAL MATCH (n)-[r]-(p:PropertyTypeSchema)-[rp]-() OPTIONAL MATCH (n)-[r2]-() DELETE n,r,p,r2,rp")
	}

	public Iterable<Schema> findAll(String projectName) {
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

	public SchemaRoot findRoot() {
		return fg.v().has(SchemaRootImpl.class).nextOrDefault(SchemaRootImpl.class, null);
	}

	public Schema findByName(String name) {
		return fg.v().has("name", name).has(SchemaImpl.class).nextOrDefault(SchemaImpl.class, null);
	}

	public Schema findOne(Object id) {
		Vertex vertex = fg.getVertex(id);
		if (vertex != null) {
			fg.frameElement(vertex, SchemaImpl.class);
		}
		return null;
	}

	public Schema findByUUID(String uuid) {
		return fg.v().has("uuid", uuid).nextOrDefault(SchemaImpl.class, null);
	}

	public List<? extends SchemaImpl> findAll() {
		return fg.v().has(SchemaImpl.class).toListExplicit(SchemaImpl.class);
	}

	public void delete(Schema schema) {
		((SchemaImpl) schema).getVertex().remove();
	}

}
