package com.gentics.mesh.core.data.model.generic;

import java.util.List;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.gentics.mesh.core.data.model.tinkerpop.Translated;

public class GenericPropertyContainer extends GenericNode {

	public List<Translated> getI18nTranslations() {
		return out(BasicRelationships.HAS_OBJECT_SCHEMA).toList(Translated.class);
	}

	//TODO may be better to use I18nProperties directly
	//	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public void addI18nTranslation(Translated translation) {
		addEdge(BasicRelationships.HAS_OBJECT_SCHEMA, Translated.class);
	}

	//	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	public void setSchema(ObjectSchema schema) {

	}

	public ObjectSchema getSchema() {
		return out(BasicRelationships.HAS_OBJECT_SCHEMA).next(ObjectSchema.class);
	}

}
