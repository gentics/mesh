package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.Map;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface I18NProperties extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUT)
	public Language getLanguage();

	@DynamicProperties
	public Map<String, String> getProperties();

	@DynamicProperties
	public String getProperty(String key);

	@DynamicProperties
	public void setProperty(String key, String value);

	@DynamicProperties
	public void removeProperty(String key);
}
