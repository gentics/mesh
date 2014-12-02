package com.gentics.vertx.cailun.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface PageRepository extends GraphRepository<Page> {

	@Query("MATCH (page:_Page)<-[:`TAGGED`]-(tag:Tag) RETURN page")
	public List<Page> findPagesWithTags();

	@Query("MATCH (page:_Page) RETURN page")
	public List<Page> findAllPages();

	@Query("MATCH (page:_Page) WHERE id(page) = {0} MERGE (tag:_Tag { name:{1} }) WITH page, tag MERGE (tag)-[r:TAGGED]->(page) RETURN tag")
	public Tag tagPage(Long id, String name);

	@Query("START n=node(*) MATCH n-[rel:TAGGED]->r WHERE n.id={0} AND r.name={1} DELETE rel")
	public Tag untag(Long id, String name);

	@Query("MATCH (page:Page {name:'test111'}), (tag:Tag {name:'test'}) MATCH (tag)-[rel:`TAGGED`]->(page) return rel")
	public Tag getTag(Long id, String name);

	@Query("MATCH (page:Page),(tag:Tag { name:'/' }), p = shortestPath((tag)-[TAGGEG]-(page)) WHERE id(page) = {0} WITH page, reduce(a='', n IN FILTER(x in nodes(p) WHERE id(page)<> id(x))| a + \"/\"+ n.name) as path return substring(path,2,length(path)) + \"/\" + page.filename")
	public String getPath(Long id);
}
