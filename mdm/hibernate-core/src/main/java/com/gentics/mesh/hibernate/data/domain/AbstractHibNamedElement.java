package com.gentics.mesh.hibernate.data.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Common part for the entities, that consider a human-readable name.
 * 
 * @author plyhun
 *
 * @param <R>
 */
@MappedSuperclass
public abstract class AbstractHibNamedElement<R extends RestModel> extends AbstractHibBucketableElement implements HibCoreElement<R>, HibNamedElement {

	@Column
	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}
