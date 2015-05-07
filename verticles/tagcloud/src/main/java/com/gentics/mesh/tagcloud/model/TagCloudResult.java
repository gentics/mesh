package com.gentics.mesh.tagcloud.model;

import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.ResultColumn;

import com.gentics.mesh.core.data.model.Tag;

@MapResult
public interface TagCloudResult {
	@ResultColumn("tag")
	Tag getTag();

	@ResultColumn("count")
	Long getCounts();
}
