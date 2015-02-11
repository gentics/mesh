package com.gentics.cailun.core.rest.model.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.CaiLunNode;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class Role extends CaiLunNode {

	private static final long serialVersionUID = -6696156556292877992L;

	private String name;

	public Role(String name) {
		this.name = name;
	}

}
