package com.gentics.cailun.core.rest.model.auth.basic;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.gentics.cailun.core.rest.model.GenericNode;
import com.gentics.cailun.core.rest.model.auth.AbstractPermission;
import com.gentics.cailun.core.rest.model.auth.Role;

@NoArgsConstructor
@Data
public class BasicPermission extends AbstractPermission {

	private static final long serialVersionUID = 491752627336037999L;

	static enum BasicPermissionTypes {
	}

	private GenericNode targetNode = null;
	private BasicPermissionTypes typeToCheck = null;

	private boolean read = false;
	private boolean write = false;
	private boolean delete = false;
	private boolean create = false;

	public BasicPermission(Role role, GenericNode targetNode) {
		super(role, targetNode);
	}

}
