package com.gentics.cailun.core.repository.generic;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.generic.GenericContent;
import com.gentics.cailun.core.data.model.generic.GenericTag;
import com.gentics.cailun.core.repository.action.GenericContentRepositoryActions;

public interface GenericContentRepository<T extends GenericContent> extends GenericPropertyContainerRepository<T>, GenericContentRepositoryActions<T> {

	@Query("MATCH (content:Content)<-[:`TAGGED`]-(tag:Tag) RETURN content")
	public List<GenericContent> findContentsWithTags();

	@Query("MATCH (content:Content) RETURN content")
	public List<GenericContent> findAllGenericContents();

	@Query("MATCH (content:Content) WHERE content.uuid = {0} MERGE (tag:_Tag { name:{1} }) WITH content, tag MERGE (tag)-[r:TAGGED]->(content) RETURN tag")
	public GenericTag tagGenericContent(String uuid, String name);

	@Query("START n=node(*) MATCH n-[rel:TAGGED]->r WHERE n.uuid={0} AND r.name={1} DELETE rel")
	public GenericTag untag(String uuid, String name);

	@Query("MATCH (content:Content {name:'test111'}), (tag:Tag {name:'test'}) MATCH (tag)-[rel:`TAGGED`]->(content) return rel")
	public GenericTag getTag(String uuid, String name);

	// TODO speedup this query, reduce calls, cache query?
	// @Query("MATCH (content:GenericContent),(tag:Tag { name:'/' }), p = shortestPath((tag)-[:TAGGED]-(content)) WHERE id(content) = {0} WITH content, reduce(a='', n IN FILTER(x in nodes(p) WHERE id(content)<> id(x))| a + \"/\"+ n.name) as path return substring(path,2,length(path)) + \"/\" + content.filename")
	// public String getPath(Long id);

	@Query("MATCH (n:Content)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	Iterable<T> findAll(String projectName);
	

}
