package com.gentics.cailun.core.rest.model.auth;

import java.util.Collection;
import java.util.HashSet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.gentics.cailun.core.rest.model.AbstractPersistable;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class Role extends AbstractPersistable {

	private static final long serialVersionUID = -6696156556292877992L;

	private String name;
	
	// @Fetch
	@RelatedToVia(type = AbstractPermissionRelationship.RELATION_KEYWORD, direction = Direction.OUTGOING, elementClass = AbstractPermissionRelationship.class)
	private Collection<AbstractPermissionRelationship> permissions = new HashSet<>();

	public Role(String name) {
		this.name = name;
	}

}
