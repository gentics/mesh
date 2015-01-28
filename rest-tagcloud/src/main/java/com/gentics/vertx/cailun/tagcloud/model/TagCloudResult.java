package com.gentics.vertx.cailun.tagcloud.model;

import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.ResultColumn;

import com.gentics.vertx.cailun.tag.model.Tag;

@MapResult
public interface TagCloudResult {
	@ResultColumn("tag")
	Tag getTag();

	@ResultColumn("count")
	Long getCounts();
}
