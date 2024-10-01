package com.gentics.mesh.hibernate.data.domain;

import java.util.Map;

import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.HibSchemaFieldChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * The change chain part implementation for the (micro)schema change entity.
 * 
 * @author plyhun
 *
 */
@MappedSuperclass
public abstract class AbstractHibSchemaFieldChange extends HibSchemaChangeImpl implements HibSchemaFieldChange {

	private static final long serialVersionUID = -3171193342367657790L;

	@Override
	public HibSchemaChange<?> getNextChange() {
		return nextChange == null ? null : nextChange.intoSchemaChange();
	}

	@Override
	public HibSchemaChange<FieldSchemaContainer> setNextChange(HibSchemaChange<?> change) {
		nextChange = HibSchemaChangeImpl.intoEntity(change);
		if (nextChange != null) {
			nextChange.previousChange = this;
		}
		return this;
	}

	@Override
	public HibSchemaChange<?> getPreviousChange() {
		return previousChange == null ? null : previousChange.intoSchemaChange();
	}

	@Override
	public HibSchemaChange<FieldSchemaContainer> setPreviousChange(HibSchemaChange<?> change) {
		previousChange = HibSchemaChangeImpl.intoEntity(change);
		if (previousChange != null) {
			previousChange.nextChange = this;
		}
		return this;
	}

	@Override
	public <R extends HibFieldSchemaVersionElement<?, ?, ?, ?, ?>> R getNextContainerVersion() {
		return super.getNextContainerVersion();
	}

	@Override
	public <R extends HibFieldSchemaVersionElement<?, ?, ?, ?, ?>> R getPreviousContainerVersion() {
		return super.getPreviousContainerVersion();
	}

	@Override
	public HibSchemaChange<FieldSchemaContainer> setPreviousContainerVersion(
			HibFieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion) {
		super.setPreviousContainerVersionInner(containerVersion);
		if (containerVersion != null) {
			containerVersion.setNextChange(this);
		}
		return this;
	}

	@Override
	public HibSchemaChange<FieldSchemaContainer> setNextSchemaContainerVersion(
			HibFieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion) {
		super.setNextSchemaContainerVersionInner(containerVersion);
		if (containerVersion != null) {
			containerVersion.setPreviousChange(this);
		}
		return this;
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return super.getOperation();
	}

	@Override
	public void setRestProperty(String key, Object value) {
		super.setRestProperty(key, value);
	}

	@Override
	public <R> R getRestProperty(String key) {
		return super.getRestProperty(key);
	}

	@Override
	public <R> Map<String, R> getRestProperties() {
		return super.getRestProperties();
	}

}
