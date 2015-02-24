package com.gentics.cailun.core.data.model.generic;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class GenericFile<T extends GenericTag<T,F>, F extends GenericFile<T,F>> extends GenericPropertyContainer {

	private static final long serialVersionUID = -8945772390192195270L;

	public static final String FILENAME_KEYWORD = "filename";

	@RelatedTo(type = BasicRelationships.HAS_SUB_TAG, direction = Direction.OUTGOING, elementClass = GenericTag.class)
	private Set<T> tags = new HashSet<>();

	public String getFilename(Language language) {
		return getProperty(language, FILENAME_KEYWORD);
	}

	public Set<T> getTags() {
		return tags;
	}

	public void addTag(T tag) {
		tags.add(tag);
	}

}
