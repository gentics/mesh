package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;

import java.awt.print.Pageable;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.MicroPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.PropertyType;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
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
		return fg.v().has("uuid", uuid).nextOrDefault(Schema.class, null);
	}

	public Schema findByName(String projectName, String name) {
		if (StringUtils.isEmpty(projectName) || StringUtils.isEmpty(name)) {
			throw new NullPointerException("name or project name null");
		}
		return fg.v().has("name", name).has(Schema.class).mark().out(ASSIGNED_TO_PROJECT).has("name", projectName).back().nextOrDefault(Schema.class, null);
	}

	public void deleteByUUID(String uuid) {
	}

	public Page<Schema> findAllVisible(MeshUser requestUser, PagingInfo pagingInfo) {
		// return findAll(requestUser, new MeshPageRequest(pagingInfo));
		return null;
	}

	public BasicPropertyType getPropertyTypeSchema(String typeKey) {
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

	public Page<Schema> findAll(MeshUser requestUser, Pageable pageable) {
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return schema ORDER BY schema.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return count(schema)")
		return null;
	}

	public SchemaRoot findRoot() {
		return fg.v().nextOrDefault(SchemaRoot.class, null);
	}

	public Schema findByName(String name) {
		return fg.v().has("name", name).nextOrDefault(Schema.class, null);
	}

	public Schema create(String name) {
		Schema schema = fg.addFramedVertex(Schema.class);
		schema.setName(name);
		SchemaRoot root = findRoot();
		root.addSchema(schema);
		return schema;
	}

	public SchemaRoot createRoot() {
		SchemaRoot root = fg.addFramedVertex(SchemaRoot.class);
		return root;
	}

	public BasicPropertyType create(String key, PropertyType type) {
		BasicPropertyType schemaType = fg.addFramedVertex(BasicPropertyType.class);
		schemaType.setKey(key);
		schemaType.setType(type);
		return schemaType;
	}

	public MicroPropertyType createMicroPropertyTypeSchema(String key) {
		MicroPropertyType type = fg.addFramedVertex(MicroPropertyType.class);
		type.setKey(key);
		type.setType(PropertyType.MICROSCHEMA);
		return type;
	}

	public BasicPropertyType createBasicPropertyTypeSchema(String key, PropertyType type) {
		BasicPropertyType propertType = fg.addFramedVertex(BasicPropertyType.class);
		propertType.setKey(key);
		propertType.setType(type);
		return propertType;
	}

	public BasicPropertyType createListPropertyTypeSchema(String key) {
		BasicPropertyType type = fg.addFramedVertex(BasicPropertyType.class);
		type.setKey(key);
		return type;
	}

	public Schema findOne(Long id) {
		Vertex vertex = fg.getVertex(id);
		if (vertex != null) {
			fg.frameElement(vertex, Schema.class);
		}
		return null;
	}

	public Schema findByUUID(String uuid) {
		return fg.v().has("uuid", uuid).nextOrDefault(Schema.class, null);
	}

	public List<? extends Schema> findAll() {
		return fg.v().has(Schema.class).toListExplicit(Schema.class);
	}

	public void delete(Schema schema) {
		schema.getVertex().remove();
	}

}
