package com.gentics.cailun.core.rest.model.generic;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NodeEntity
public class GenericFile extends GenericNode {

	private static final long serialVersionUID = -8945772390192195270L;

	public static final String FILENAME_KEYWORD = "filename";

	@RelatedTo(type = BasicRelationships.HAS_SUB_TAG, direction = Direction.OUTGOING, elementClass = GenericTag.class)
	private Set<GenericTag> tags = new HashSet<>();

	public String getFilename(Language language) {
		return getI18NProperty(language, FILENAME_KEYWORD);
	}

	public Set<GenericTag> getTags() {
		return tags;
	}

	public void addTag(GenericTag tag) {
		tags.add(tag);
	}

}
