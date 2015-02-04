package com.gentics.cailun.core.rest.model.auth;

import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.AbstractPersistable;
import com.gentics.cailun.core.rest.model.GenericNode;

@NodeEntity
@NoArgsConstructor
public abstract class AbstractPermission extends AbstractPersistable {

	private static final long serialVersionUID = 8304718445043642942L;

	@RelatedTo(type = AuthRelationships.HAS_PERMISSION, direction = Direction.INCOMING, elementClass = Role.class)
	private Role role;

	@RelatedTo(type = AuthRelationships.ASSIGNED_TO, direction = Direction.INCOMING, elementClass = GenericNode.class)
	private GenericNode targetNode;

	public AbstractPermission(Role role, GenericNode targetNode) {
		this.role = role;
		this.targetNode = targetNode;
	}
}
