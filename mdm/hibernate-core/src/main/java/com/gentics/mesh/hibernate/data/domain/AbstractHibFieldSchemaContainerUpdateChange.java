package com.gentics.mesh.hibernate.data.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.schema.HibFieldSchemaContainerUpdateChange;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Common part for (micro)schema update changes entity.
 * 
 * @author plyhun
 *
 * @param <T> the container entity type
 */
@MappedSuperclass
public abstract class AbstractHibFieldSchemaContainerUpdateChange<T extends FieldSchemaContainer> extends HibSchemaChangeImpl
		implements HibFieldSchemaContainerUpdateChange<T> {

	private static final long serialVersionUID = -8640864614394546409L;

	@Override
	public Boolean getNoIndex() {
		return getRestProperty(SchemaChangeModel.NO_INDEX_KEY);
	}

	@Override
	public void setNoIndex(Boolean noIndex) {
		setRestProperty(SchemaChangeModel.NO_INDEX_KEY, noIndex);
	}

	@Override
	public String getName() {
		return getRestProperty(SchemaChangeModel.NAME_KEY);
	}

	@Override
	public void setName(String name) {
		setRestProperty(SchemaChangeModel.NAME_KEY, name);
	}

	@Override
	public String getDescription() {
		return getRestProperty(SchemaChangeModel.DESCRIPTION_KEY);
	}

	@Override
	public void setDescription(String description) {
		setRestProperty(SchemaChangeModel.DESCRIPTION_KEY, description);
	}

	@Override
	public List<String> getOrder() {
		Object[] fieldNames = getRestProperty(SchemaChangeModel.FIELD_ORDER_KEY);
		if (fieldNames == null) {
			return null;
		} else {
			return Arrays.stream(fieldNames).map(Object::toString).collect(Collectors.toList());
		}
	}

	@Override
	public void setOrder(String... fieldNames) {
		setRestProperty(SchemaChangeModel.FIELD_ORDER_KEY, fieldNames);
	}

	@Override
	public HibSchemaChange<?> getNextChange() {
		return nextChange != null ? nextChange.intoSchemaChange() : null;
	}

	@Override
	public HibSchemaChange<T> setNextChange(HibSchemaChange<?> change) {
		nextChange = HibSchemaChangeImpl.intoEntity(change);
		if (nextChange != null) {
			nextChange.previousChange = this;
		}
		return this;
	}

	@Override
	public HibSchemaChange<?> getPreviousChange() {
		return previousChange != null ? previousChange.intoSchemaChange() : null;
	}

	@Override
	public HibSchemaChange<T> setPreviousChange(HibSchemaChange<?> change) {
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
	public HibSchemaChange<T> setPreviousContainerVersion(
			HibFieldSchemaVersionElement<?, ?, ?, ?, ?> containerVersion) {
		super.setPreviousContainerVersionInner(containerVersion);
		if (containerVersion != null) {
			containerVersion.setNextChange(this);
		}
		return this;
	}

	@Override
	public HibSchemaChange<T> setNextSchemaContainerVersion(
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
