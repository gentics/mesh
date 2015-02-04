package com.gentics.cailun.core.rest.model.auth;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.AbstractPersistable;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class Role extends AbstractPersistable {

	private static final long serialVersionUID = -6696156556292877992L;

	private String name;

	// @Fetch
	@RelatedTo(type = AuthRelationships.HAS_PERMISSION, direction = Direction.OUTGOING, elementClass = AbstractPermission.class)
	private Set<AbstractPermission> permissions = new HashSet<>();

	public Role(String name) {
		this.name = name;
	}

	public void addPermission(AbstractPermission perm) {
		this.permissions.add(perm);
	}

}
