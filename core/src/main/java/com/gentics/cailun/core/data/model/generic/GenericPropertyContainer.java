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
import com.gentics.cailun.core.data.model.Tag;
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

	@RelatedTo(type = BasicRelationships.HAS_TAG, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> tags = new HashSet<>();

	@RelatedTo(type = BasicRelationships.HAS_PARENT_TAG, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Tag parentTag;

	public Tag getParentTag() {
		return parentTag;
	}

	public void setParent(Tag tag) {
		this.parentTag = tag;
	}

	public Set<Translated> getI18nTranslations() {
		return i18nTranslations;
	}

	public void setSchema(ObjectSchema schema) {
		this.schema = schema;
	}

	public ObjectSchema getSchema() {
		return schema;
	}

	public void addTag(Tag tag) {
		tags.add(tag);
	}

	public boolean removeTag(Tag tag) {
		return tags.remove(tag);
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	public boolean hasTag(Tag tag) {
		for (Tag childTag : tags) {
			if (tag.equals(childTag)) {
				return true;
			}
		}
		return false;
	}

}
