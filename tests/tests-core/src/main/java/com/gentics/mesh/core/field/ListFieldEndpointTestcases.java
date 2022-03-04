package com.gentics.mesh.core.field;

/**
 * Test case definition for list fields
 */
public interface ListFieldEndpointTestcases {

	/**
	 * Null values are not allowed in lists and thus the request which contains a null value within a list should fail. Assert that this error is throw during a
	 * node update request.
	 */
	void testNullValueInListOnUpdate();

	/**
	 * Null values are not allowed in lists and thus the request which contains a null value within a list should fail. Assert that this error is throw during a
	 * node create request.
	 */
	void testNullValueInListOnCreate();
}
