package com.gentics.cailun.core.data.model.generic;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.core.data.model.relationship.Translated;

@NodeEntity
public class GenericPropertyContainer extends GenericNode {

	private static final long serialVersionUID = 7551202734708358487L;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@RelatedTo(type = BasicRelationships.HAS_SCHEMA, direction = Direction.OUTGOING, elementClass = ObjectSchema.class)
	protected ObjectSchema schema;

	@RelatedToVia(type = BasicRelationships.HAS_I18N_PROPERTIES, direction = Direction.OUTGOING, elementClass = Translated.class)
	protected Set<Translated> i18nTranslations = new HashSet<>();

	public Set<Translated> getI18nTranslations() {
		return i18nTranslations;
	}

	public void setSchema(ObjectSchema schema) {
		this.schema = schema;
	}

	public ObjectSchema getSchema() {
		return schema;
	}

}
