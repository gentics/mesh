package com.gentics.vertx.cailun.perm.model;

import java.util.Collection;
import java.util.HashSet;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.gentics.vertx.cailun.base.model.AbstractPersistable;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class Role extends AbstractPersistable {

	private static final long serialVersionUID = -6696156556292877992L;

	private String name;
	
	// @Fetch
	@RelatedToVia(type = PermissionSet.RELATION_KEYWORD, direction = Direction.OUTGOING, elementClass = PermissionSet.class)
	private Collection<PermissionSet> permissions = new HashSet<>();

	public Role(String name) {
		this.name = name;
	}

}
