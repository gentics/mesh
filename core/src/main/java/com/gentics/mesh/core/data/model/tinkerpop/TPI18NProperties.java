package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.Properties;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPI18NProperties extends TPAbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUT)
	public TPLanguage getLanguage();

	@DynamicProperties
	public Properties getProperties();

	@DynamicProperties
	public String getProperty(String key);

	@DynamicProperties
	public void setProperty(String key, String value);
}
