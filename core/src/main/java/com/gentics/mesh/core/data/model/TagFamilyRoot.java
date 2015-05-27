package com.gentics.mesh.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

/**
 * Aggregation root node for tags
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class TagFamilyRoot extends AbstractPersistable {

	private static final long serialVersionUID = -8368745654542459791L;

	@RelatedTo(type = BasicRelationships.HAS_TAG, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> tags = new HashSet<>();

}
