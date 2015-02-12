package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

/**
 * A content is basically a tag which can't have any child tags.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class LocalizedContent extends CaiLunNode {

	private static final long serialVersionUID = 7918024043584207109L;

	@RelatedTo(type = BasicRelationships.HAS_LOCALIZED_CONTENT, elementClass = Content.class, direction = Direction.OUTGOING)
	private Set<Content> localizedContents = new HashSet<>();

	public void addContent(Content content) {
		this.localizedContents.add(content);
	}

	public Set<Content> getContents() {
		return localizedContents;
	}

}
