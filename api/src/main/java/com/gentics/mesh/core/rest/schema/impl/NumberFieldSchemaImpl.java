package com.gentics.mesh.core.rest.schema.impl;

import java.util.Optional;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;

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
	public Optional<SchemaChangeModelImpl> compareTo(FieldSchema fieldSchema) {
		// TODO Auto-generated method stub
		return null;
	}
}
