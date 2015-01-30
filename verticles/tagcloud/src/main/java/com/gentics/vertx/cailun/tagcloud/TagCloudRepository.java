package com.gentics.vertx.cailun.tagcloud;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.rest.model.Page;
import com.gentics.vertx.cailun.tagcloud.model.TagCloudResult;

public interface TagCloudRepository extends GraphRepository<Page> {

	/**
	 * Return the count of relationships from all tags to pages
	 */
	@Query("MATCH (n:Tag)<-[r:TAGGED]-(x:Page) RETURN n as tag, COUNT(r) as count ORDER BY COUNT(r) DESC")
	public List<TagCloudResult> getTagCloudInfo();

}
