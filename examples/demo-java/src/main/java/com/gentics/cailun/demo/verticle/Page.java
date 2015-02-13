package com.gentics.cailun.demo.verticle;

import org.springframework.data.neo4j.annotation.NodeEntity;

import lombok.NoArgsConstructor;

import com.gentics.cailun.core.rest.model.Content;

@NoArgsConstructor
@NodeEntity
public class Page extends Content {

	private static final long serialVersionUID = -4927498999985839348L;

}
