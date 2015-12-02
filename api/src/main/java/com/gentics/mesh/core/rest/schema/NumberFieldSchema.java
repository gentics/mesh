package com.gentics.mesh.core.rest.schema;

/**
 * A number field schema is a schema definition for a number field.
 *
 */
public interface NumberFieldSchema extends FieldSchema {

	/**
	 * Return the stepping for the numbers.
	 * 
	 * @return Stepping for numbers
	 */
	Float getStep();

	/**
	 * Set the stepping for the numbers.
	 * 
	 * @param step
	 * @return fluent api
	 */
	NumberFieldSchema setStep(Float step);

	/**
	 * Return the lower limit for numbers.
	 * 
	 * @return Lower limit
	 */
	Integer getMin();

	/**
	 * Set the lower limit for numbers.
	 * 
	 * @param min
	 *            Lower limit
	 * @return Fluent API
	 */
	NumberFieldSchema setMin(Integer min);

	/**
	 * Return the max limit for numbers.
	 * 
	 * @return Upper limit
	 */
	Integer getMax();

	/**
	 * Set max limit for numbers.
	 * 
	 * @param max
	 *            Upper limit
	 * @return Fluent API
	 */
	NumberFieldSchema setMax(Integer max);

}
