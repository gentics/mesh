package com.gentics.cailun.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

/**
 * A project is the root element for a tag hierarchy.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class Project extends GenericNode {

	private static final long serialVersionUID = -3565883313897315008L;

	@Indexed(unique = true)
	protected String name;

	@SuppressWarnings("unused")
	private Project() {
	}

	public Project(String name) {
		this.name = name;
	}

	@RelatedTo(type = BasicRelationships.HAS_ROOT_TAG, direction = Direction.OUTGOING, elementClass = RootTag.class)
	private RootTag rootTag;

	@RelatedTo(type = BasicRelationships.HAS_OBJECT_SCHEMA, direction = Direction.OUTGOING, elementClass = ObjectSchema.class)
	private Set<ObjectSchema> objectSchema = new HashSet<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public RootTag getRootTag() {
		return rootTag;
	}

	public void setRootTag(RootTag rootTag) {
		this.rootTag = rootTag;
	}

}
