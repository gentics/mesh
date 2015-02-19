package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.generic.AbstractPersistable;

@NodeEntity
public class PropertySchema extends AbstractPersistable {

	private static final long serialVersionUID = 6242394504946538888L;

	PropertyType type;
	String key;
	// TODO i18n?
	String description;

}
