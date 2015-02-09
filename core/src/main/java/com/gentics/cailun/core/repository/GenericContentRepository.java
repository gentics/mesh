package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.rest.model.GenericContent;
import com.gentics.cailun.core.rest.model.Tag;

public interface GenericContentRepository extends GraphRepository<GenericContent> {

	@Query("MATCH (content:_GenericContent)<-[:`TAGGED`]-(tag:Tag) RETURN content")
	public List<GenericContent> findGenericContentsWithTags();

	@Query("MATCH (content:_GenericContent) RETURN content")
	public List<GenericContent> findAllGenericContents();

	@Query("MATCH (content:_GenericContent) WHERE content.uuid = {0} MERGE (tag:_Tag { name:{1} }) WITH content, tag MERGE (tag)-[r:TAGGED]->(content) RETURN tag")
	public Tag tagGenericContent(String uuid, String name);

	@Query("START n=node(*) MATCH n-[rel:TAGGED]->r WHERE n.uuid={0} AND r.name={1} DELETE rel")
	public Tag untag(String uuid, String name);

	@Query("MATCH (content:GenericContent {name:'test111'}), (tag:Tag {name:'test'}) MATCH (tag)-[rel:`TAGGED`]->(content) return rel")
	public Tag getTag(String uuid, String name);

	// TODO speedup this query, reduce calls, cache query?
//	@Query("MATCH (content:GenericContent),(tag:Tag { name:'/' }), p = shortestPath((tag)-[:TAGGED]-(content)) WHERE id(content) = {0} WITH content, reduce(a='', n IN FILTER(x in nodes(p) WHERE id(content)<> id(x))| a + \"/\"+ n.name) as path return substring(path,2,length(path)) + \"/\" + content.filename")
//	public String getPath(Long id);

	@Query("MATCH (content:GenericContent {uuid: {0}} return content")
	public GenericContent findByUUID(String uuid);

	@Query("MATCH (n:GenericContent {uuid: {0}} DELETE n")
	public void delete(String uuid);

}
