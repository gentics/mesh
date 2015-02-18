package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.generic.GenericNode;
import com.gentics.cailun.core.rest.model.generic.GenericTag;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

/**
 * A project is the root element for a tag, user,group hierarchy.
 * 
 * @author johannes2
 *
 */
@Data
@NodeEntity
public class Project extends GenericNode {

	private static final long serialVersionUID = -3565883313897315008L;

	@Fetch
	protected String name;

	@SuppressWarnings("unused")
	private Project() {
	}

	public Project(String name) {
		this.name = name;
	}

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_ROOT_TAG, direction = Direction.OUTGOING, elementClass = GenericTag.class)
	private GenericTag rootTag;

	@Fetch
	@RelatedTo(type = AuthRelationships.HAS_ROLE, direction = Direction.OUTGOING, elementClass = Role.class)
	private Set<Role> roles = new HashSet<>();

}
