package com.gentics.cailun.util;

import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;

public final class PermissionUtils {

	public static String convert(AbstractPersistable node, PermissionType type) {
		return node.getId() + "#" + type.getPropertyName();
	}

}
