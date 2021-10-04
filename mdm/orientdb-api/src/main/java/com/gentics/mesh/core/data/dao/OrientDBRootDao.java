package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.common.RestModel;

public interface OrientDBRootDao<R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> extends RootDaoPersistable<R, L> {

	@Override
	default L mergeIntoPersisted(R root, L element) {
		return element;
	}
}
