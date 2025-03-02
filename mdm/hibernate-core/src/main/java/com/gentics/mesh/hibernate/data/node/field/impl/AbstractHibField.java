package com.gentics.mesh.hibernate.data.node.field.impl;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * The very base of the Hibernate field entity.
 * 
 * @author plyhun
 *
 */
public abstract class AbstractHibField implements HibField {

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
	 */
	public void onFieldDeleted(HibernateTx tx) {}

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
