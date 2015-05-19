package com.gentics.mesh.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.repository.generic.GenericPropertyContainerRepository;

public interface MeshNodeRepository extends GenericPropertyContainerRepository<MeshNode> {

	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (node)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE l.languageTag IN {2} AND id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "WITH p, node "
			+ "ORDER by p.`properties-name` desc " + "RETURN DISTINCT node", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (node)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE l.languageTag IN {2} AND id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "RETURN count(DISTINCT node)"

	)
	Page<MeshNode> findAll(User requestUser, String projectName, List<String> languageTags, Pageable pageable);

	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (node)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "WITH p, node "
			+ "ORDER by p.`properties-name` desc " + "RETURN DISTINCT node", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (node)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} " + "RETURN count(DISTINCT node)"

	)
	Page<MeshNode> findAll(User requestUser, String projectName, Pageable pageable);

	@Query("MATCH (node:MeshNode)<-[:`TAGGED`]-(tag:Tag) RETURN node")
	public List<MeshNode> findNodesWithTags();

	@Query("MATCH (node:MeshNode) RETURN node")
	public List<MeshNode> findAllNodes();

	@Query("MATCH (node:MeshNode) WHERE node.uuid = {0} MERGE (tag:_Tag { name:{1} }) WITH node, tag MERGE (tag)-[r:TAGGED]->(node) RETURN tag")
	public Tag tagGenericNode(String uuid, String name);

	@Query("START n=node(*) MATCH n-[rel:TAGGED]->r WHERE n.uuid={0} AND r.name={1} DELETE rel")
	public Tag untag(String uuid, String name);

	@Query("MATCH (node:MeshNode {name:'test111'}), (tag:Tag {name:'test'}) MATCH (tag)-[rel:`TAGGED`]->(node) return rel")
	public Tag getTag(String uuid, String name);

	// TODO speedup this query, reduce calls, cache query?
	// @Query("MATCH (node:GenericMeshNode),(tag:Tag { name:'/' }), p = shortestPath((tag)-[:TAGGED]-(node)) WHERE id(node) = {0} WITH node, reduce(a='', n IN FILTER(x in nodes(p) WHERE id(node)<> id(x))| a + \"/\"+ n.name) as path return substring(path,2,length(path)) + \"/\" + node.filename")
	// public String getPath(Long id);

	@Query("MATCH (n:MeshNode)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	Iterable<MeshNode> findAll(String projectName);
}
