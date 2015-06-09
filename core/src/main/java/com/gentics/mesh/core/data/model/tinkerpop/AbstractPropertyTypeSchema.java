package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface AbstractPropertyTypeSchema extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_I18N_PROPERTIES, direction = Direction.OUT)
	public Iterable<Translated> getI18nTranslations();

	@Property("type")
	public String getType();

	@Property("type")
	public void setType(String type);

	@Property("key")
	public String getKey();

	@Property("key")
	public void setKey(String key);

	@Property("description")
	public String getDescription();

	@Property("description")
	public void setDescription(String description);

	@Property("displayName")
	public String getDisplayName();

	@Property("displayName")
	public void setDisplayName(String displayName);

	@Property("order")
	public int getOrder();

	@Property("order")
	public void setOrder(int order);

}
