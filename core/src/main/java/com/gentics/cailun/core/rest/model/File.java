package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NodeEntity
public class File extends CaiLunNode {

	private static final long serialVersionUID = -8945772390192195270L;

	public static final String FILENAME_KEYWORD = "filename";

	@RelatedTo(type = BasicRelationships.HAS_SUB_TAG, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> tags = new HashSet<>();

	public String getFilename(Language language) {
		return getI18NProperty(language, FILENAME_KEYWORD);
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void addTag(Tag tag) {
		tags.add(tag);
	}

}
