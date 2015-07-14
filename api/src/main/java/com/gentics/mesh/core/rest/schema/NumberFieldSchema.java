package com.gentics.mesh.core.rest.schema;


public interface NumberFieldSchema extends MicroschemaListableFieldSchema {

	void setStep(Float step);

	Float getStep();

	void setMin(Integer min);

	Integer getMin();

	void setMax(Integer max);

	Integer getMax();

}
