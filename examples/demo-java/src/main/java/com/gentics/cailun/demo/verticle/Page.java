package com.gentics.cailun.demo.verticle;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;

@NodeEntity
public class Page extends Content<LocalizedPage> {

	private static final long serialVersionUID = -4927498999985839348L;

	public Page(Language language, String name, String filename) {
		super(language, name, filename);
	}



}
