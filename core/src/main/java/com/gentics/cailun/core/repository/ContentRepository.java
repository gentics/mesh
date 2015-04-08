package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.generic.GenericPropertyContainerRepository;

public interface ContentRepository extends GenericPropertyContainerRepository<Content> {

	@Query(
			value="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
					+ "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
					+ "WHERE l.languageTag IN {2} AND id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
					+ "WITH p, content "
					+ "ORDER by p.`properties-name` desc "
					+ "RETURN DISTINCT content",
			countQuery="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
					+ "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
					+ "WHERE l.languageTag IN {2} AND id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
					+ "RETURN count(DISTINCT content)"
			
		)
	Page<Content> findAll(User requestUser, String projectName, List<String> languageTags,Pageable pageable);
	
	
	@Query(
			value="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
					+ "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
					+ "WHERE id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
					+ "WITH p, content "
					+ "ORDER by p.`properties-name` desc "
					+ "RETURN DISTINCT content",
			countQuery="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
					+ "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
					+ "WHERE id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
					+ "RETURN count(DISTINCT content)"
			
		)
	Page<Content> findAll(User requestUser, String projectName, Pageable pageable);
	
	@Query("MATCH (content:Content)<-[:`TAGGED`]-(tag:Tag) RETURN content")
	public List<Content> findContentsWithTags();

	@Query("MATCH (content:Content) RETURN content")
	public List<Content> findAllGenericContents();

	@Query("MATCH (content:Content) WHERE content.uuid = {0} MERGE (tag:_Tag { name:{1} }) WITH content, tag MERGE (tag)-[r:TAGGED]->(content) RETURN tag")
	public Tag tagGenericContent(String uuid, String name);

	@Query("START n=node(*) MATCH n-[rel:TAGGED]->r WHERE n.uuid={0} AND r.name={1} DELETE rel")
	public Tag untag(String uuid, String name);

	@Query("MATCH (content:Content {name:'test111'}), (tag:Tag {name:'test'}) MATCH (tag)-[rel:`TAGGED`]->(content) return rel")
	public Tag getTag(String uuid, String name);

	// TODO speedup this query, reduce calls, cache query?
	// @Query("MATCH (content:GenericContent),(tag:Tag { name:'/' }), p = shortestPath((tag)-[:TAGGED]-(content)) WHERE id(content) = {0} WITH content, reduce(a='', n IN FILTER(x in nodes(p) WHERE id(content)<> id(x))| a + \"/\"+ n.name) as path return substring(path,2,length(path)) + \"/\" + content.filename")
	// public String getPath(Long id);

	@Query("MATCH (n:Content)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	Iterable<Content> findAll(String projectName);
}
