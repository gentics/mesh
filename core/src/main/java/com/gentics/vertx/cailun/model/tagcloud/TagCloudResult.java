package com.gentics.vertx.cailun.model.tagcloud;

import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.ResultColumn;

import com.gentics.vertx.cailun.model.Tag;

@MapResult
public interface TagCloudResult {
	@ResultColumn("tag")
	Tag getTag();

	@ResultColumn("count")
	Long getCounts();
}
