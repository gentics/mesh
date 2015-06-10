package com.gentics.mesh.core.data.model.generic;

import java.util.List;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.gentics.mesh.core.data.model.tinkerpop.Translated;

public class GenericPropertyContainer extends GenericNode {

	public List<Translated> getI18nTranslations() {
		return outE(BasicRelationships.HAS_OBJECT_SCHEMA).toList(Translated.class);
	}

	//TODO may be better to use I18nProperties directly
	//	@Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	//	public void addI18nTranslation(Translated translation) {
	//		addEdge(BasicRelationships.HAS_OBJECT_SCHEMA, Translated.class);
	//	}
	public Translated addI18nTranslation(MeshVertex node, I18NProperties tagProps, Language language) {
		Translated translated = addEdge(BasicRelationships.HAS_I18N_PROPERTIES, tagProps, Translated.class);
		translated.setLanguageTag(language.getLanguageTag());
		return translated;
	}

	public void setSchema(ObjectSchema schema) {
		linkOut(schema, BasicRelationships.HAS_OBJECT_SCHEMA);
	}

	public ObjectSchema getSchema() {
		return out(BasicRelationships.HAS_OBJECT_SCHEMA).next(ObjectSchema.class);
	}

}
