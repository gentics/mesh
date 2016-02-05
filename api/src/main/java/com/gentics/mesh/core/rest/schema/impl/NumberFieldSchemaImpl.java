package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.util.Optional;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public class NumberFieldSchemaImpl extends AbstractFieldSchema implements NumberFieldSchema {

	private Integer min;

	private Integer max;

	private Float step;

	@Override
	public Integer getMax() {
		return max;
	}

	@Override
	public NumberFieldSchema setMax(Integer max) {
		this.max = max;
		return this;
	}

	@Override
	public Integer getMin() {
		return min;
	}

	@Override
	public NumberFieldSchema setMin(Integer min) {
		this.min = min;
		return this;
	}

	@Override
	public Float getStep() {
		return step;
	}

	@Override
	public NumberFieldSchema setStep(Float step) {
		this.step = step;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.NUMBER.toString();
	}

	@Override
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) {

		if (fieldSchema instanceof NumberFieldSchema) {
			NumberFieldSchema numberFieldSchema = (NumberFieldSchema) fieldSchema;

			SchemaChangeModel change = new SchemaChangeModel(UPDATEFIELD, fieldSchema.getName());
			boolean modified = false;

			// required flag:
			modified = compareRequiredField(change, numberFieldSchema, modified);

			if (modified) {
				return Optional.of(change);
			}
		} else {
			//TODO impl
		}
		return Optional.empty();
	}
}
