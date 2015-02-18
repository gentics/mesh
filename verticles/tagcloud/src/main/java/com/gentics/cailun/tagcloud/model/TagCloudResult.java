package com.gentics.cailun.tagcloud.model;

import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.ResultColumn;

import com.gentics.cailun.core.rest.model.generic.GenericTag;

@MapResult
public interface TagCloudResult {
	@ResultColumn("tag")
	GenericTag getTag();

	@ResultColumn("count")
	Long getCounts();
}
