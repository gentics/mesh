package com.gentics.mesh.hibernate.data.dao.helpers;

import com.gentics.mesh.core.data.BaseElement;

/**
 * Common join definition
 */
public abstract class AbstractRootJoin<CHILDIMPL extends BaseElement, ROOTIMPL extends BaseElement> implements RootJoin<CHILDIMPL, ROOTIMPL> {

	protected final Class<CHILDIMPL> domainClass;
	protected final Class<ROOTIMPL> rootClass;

	protected AbstractRootJoin(Class<CHILDIMPL> domainClass, Class<ROOTIMPL> rootClass) {
		this.domainClass = domainClass;
		this.rootClass = rootClass;
	}

	@Override
	public Class<CHILDIMPL> getDomainClass() {
		return domainClass;
	}

	@Override
	public Class<ROOTIMPL> getRootClass() {
		return rootClass;
	}
}
