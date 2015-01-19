package com.gentics.vertx.cailun.tagcloud.model;

import lombok.Data;

@Data
public class TagCloudEntry {

	String name;
	Long count;
	String link;
}
