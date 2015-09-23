package com.gentics.mesh.core.rest.schema;

/**
 * A number field schema is a schema definition for a number field.
 *
 */
public interface NumberFieldSchema extends MicroschemaListableFieldSchema {

	/**
	 * Set the stepping for the numbers.
	 * 
	 * @param step
	 * @return fluent api
	 */
	NumberFieldSchema setStep(Float step);

	/**
	 * Return the stepping for the numbers.
	 * 
	 * @return
	 */
	Float getStep();

	NumberFieldSchema setMin(Integer min);

	/**
	 * Return the lower limit for the numbers.
	 * 
	 * @return
	 */
	Integer getMin();

	/**
	 * Return the max limit for the numbers.
	 * 
	 * @return
	 */
	Integer getMax();

	NumberFieldSchema setMax(Integer max);

}
