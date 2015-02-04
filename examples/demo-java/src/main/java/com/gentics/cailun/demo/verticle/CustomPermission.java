package com.gentics.cailun.demo.verticle;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.gentics.cailun.core.rest.model.GenericNode;
import com.gentics.cailun.core.rest.model.auth.AbstractPermission;
import com.gentics.cailun.core.rest.model.auth.Role;

@NoArgsConstructor
@Data
public class CustomPermission extends AbstractPermission {

	private static final long serialVersionUID = 7570392688600872288L;

	private boolean customActionAllowed = false;

	public CustomPermission(Role role, GenericNode targetNode) {
		super(role, targetNode);
	}

}
