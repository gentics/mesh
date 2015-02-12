package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

/**
 * A project is the root element for a tag, user,group hierarchy.
 * 
 * @author johannes2
 *
 */
@Data
@NoArgsConstructor
@NodeEntity
public class Project extends CaiLunNode {

	private static final long serialVersionUID = -3565883313897315008L;

	@Fetch
	protected String name;

	public Project(String name) {
		this.name = name;
	}

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_ROOT_TAG, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Tag rootTag;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_ROOT_GROUP, direction = Direction.OUTGOING, elementClass = Group.class)
	private Group rootGroup;

	@Fetch
	@RelatedTo(type = AuthRelationships.HAS_ROLE, direction = Direction.OUTGOING, elementClass = Role.class)
	private Set<Role> roles = new HashSet<>();

}
