package com.gentics.mesh.core.data.model.generic;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.gentics.mesh.core.data.model.tinkerpop.Translated;
import com.tinkerpop.frames.Adjacency;

public interface GenericPropertyContainer extends GenericNode {

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public Iterable<Translated> getI18nTranslations();
	
	//TODO may be better to use I18nProperties directly
	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public void addI18nTranslation(Translated translation);
	

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public void setSchema(ObjectSchema schema);

	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public ObjectSchema getSchema();

}
