package com.gentics.mesh.core.rest.schema;


public interface NumberFieldSchema extends MicroschemaListableFieldSchema {

	NumberFieldSchema setStep(Float step);

	Float getStep();

	NumberFieldSchema setMin(Integer min);

	Integer getMin();

	NumberFieldSchema setMax(Integer max);

	Integer getMax();

}
