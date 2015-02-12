package com.gentics.cailun.tagcloud.model;

import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.ResultColumn;

import com.gentics.cailun.core.rest.model.LocalizedTag;

@MapResult
public interface TagCloudResult {
	@ResultColumn("tag")
	LocalizedTag getTag();

	@ResultColumn("count")
	Long getCounts();
}
