package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

/**
 * A content container is a container for language specific contents.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class ContentContainer extends TaggableNode {

	private static final long serialVersionUID = -4660067439732567635L;

	@RelatedTo(type = BasicRelationships.HAS_CONTENT, elementClass = LocalizedContent.class, direction = Direction.OUTGOING)
	private Set<LocalizedContent> contents = new HashSet<>();
}
