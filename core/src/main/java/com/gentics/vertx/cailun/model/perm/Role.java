package com.gentics.vertx.cailun.model.perm;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.security.core.GrantedAuthority;

import com.gentics.vertx.cailun.model.AbstractPersistable;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class Role extends AbstractPersistable implements GrantedAuthority {

	private static final long serialVersionUID = -6696156556292877992L;

	private String name;

	@Override
	public String getAuthority() {
		return name;
	}

}
