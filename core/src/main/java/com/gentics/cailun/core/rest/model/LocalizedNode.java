package com.gentics.cailun.core.rest.model;

import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NoArgsConstructor
public class LocalizedNode extends CaiLunNode {

	private static final long serialVersionUID = -6118752350728012166L;

	@Indexed
	protected String name;

	@RelatedTo(elementClass = Language.class, direction = Direction.OUTGOING, type = BasicRelationships.HAS_LANGUAGE)
	protected Language language;

	public LocalizedNode(Language language, String name) {
		this.name = name;
		this.language = language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
