package com.gentics.mesh.hibernate.data.node.field.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * The very base of the Hibernate field entity.
 * 
 * @author plyhun
 *
 */
public abstract class AbstractHibField implements Field {

	private String fieldKey;
	private final HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parentContainer;

	public AbstractHibField(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent) {
		this.fieldKey = fieldKey;
		this.parentContainer = parent;
	}

	/**
	 * Cleanup the referenced data, if required. Does nothing by default.
	 * 
	 * @param tx current transaction
	 * @param bac action context to report into.
	 */
	public void onFieldDeleted(HibernateTx tx, BulkActionContext bac) {}

	/**
	 * Get the container the field is currently attached to. May be either node content or micronode.
	 * 
	 * @return
	 */
	public HibUnmanagedFieldContainer<?, ?, ?, ?, ?> getContainer() {
		return parentContainer;
	}
	
	@Override
	public void setFieldKey(String key) {
		this.fieldKey = key;
	}

	@Override
	public String getFieldKey() {
		return fieldKey;
	}
}
