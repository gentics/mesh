package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.node.field.NumberField;

public interface NumberFieldSchema extends NumberField, FieldSchema {

	void setStep(Float step);

	Float getStep();

	void setMin(Integer min);

	Integer getMin();

	void setMax(Integer max);

	Integer getMax();

}
