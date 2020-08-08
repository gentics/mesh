package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;

public interface LoadAction<T> {

	T load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);
}
