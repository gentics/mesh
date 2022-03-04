package com.gentics.mesh.core.field;

/**
 * List of common test cases for fields.
 */
public interface FieldTestcases {

	/**
	 * Create the specific graph field and set some values which are later being updated.
	 * 
	 * Assert on a reloaded field that the update was successful.
	 * 
	 * @throws Exception
	 */
	void testFieldUpdate() throws Exception;

	/**
	 * Transform the node that contains the field.
	 * 
	 * Assert that the response object contains the expected data.
	 * 
	 * @throws Exception
	 */
	void testFieldTransformation() throws Exception;

	/**
	 * Compare two graph fields with eachother and assert that both match.
	 */
	void testEquals();

	/**
	 * Check whether the equals methods are handling null values correctly.
	 */
	void testEqualsNull();

	/**
	 * Compare rest fields with the graph field and assert different situations.
	 * 
	 * <blockquote>
	 * 
	 * <pre>
	 * graphField empty - restField null
	 * graphField set - restField set (different value)
	 * graphField set - restField set (same value)
	 * graphField set - restField set (same value, different type)
	 * </pre>
	 * 
	 * </blockquote>
	 */
	void testEqualsRestField();

	/**
	 * Invoke an update on a graph field container using a rest model which contains a null value for the node field.
	 * 
	 * The create request should not fail. No field should be created.
	 */
	void testUpdateFromRestNullOnCreate();

	/**
	 * Invoke an update on a graph field container using a rest model which contains a null value for the required node field.
	 * 
	 * The create request should fail since the value must be present in a valid format.
	 */
	void testUpdateFromRestNullOnCreateRequired();

	/**
	 * Invoke an update on a graph field container and update the field using a null field value. (e.g.: DateField.getDate()==null)
	 * 
	 * We expect the value to be removed.
	 */
	void testRemoveFieldViaNull();

	/**
	 * Invoke an update on a graph field container and update the required field using a null value.
	 * 
	 * We expect the update to fail since it is not allowed to remove required fields.
	 */
	void testRemoveRequiredFieldViaNull();

	/**
	 * Invoke an update on a graph field container using valid data. Assert that the node was correctly updated.
	 */
	void testUpdateFromRestValidSimpleValue();

	/**
	 * Test whether the field can be cloned to another field container.
	 */
	void testClone();
}
