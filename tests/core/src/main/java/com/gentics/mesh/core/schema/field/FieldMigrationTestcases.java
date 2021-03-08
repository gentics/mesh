package com.gentics.mesh.core.schema.field;

/**
 * Interface for tests for field migrations
 */
public interface FieldMigrationTestcases {
	/**
	 * Test removing the field
	 * 
	 * @throws Exception
	 */
	void testRemove() throws Exception;

	/**
	 * Test changing the field to a binary field
	 * 
	 * @throws Exception
	 */
	void testChangeToBinary() throws Exception;

	/**
	 * Test changing an empty source field to a binary field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToBinary() throws Exception;

	/**
	 * Test changing the field to a boolean field.
	 * 
	 * @throws Exception
	 */
	void testChangeToBoolean() throws Exception;

	/**
	 * Test changing an empty source field to a boolean field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToBoolean() throws Exception;

	/**
	 * Test changing to a boolean list field.
	 * 
	 * @throws Exception
	 */
	void testChangeToBooleanList() throws Exception;

	/**
	 * Test changing an empty field to a boolean list field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToBooleanList() throws Exception;

	/**
	 * Test changing to date field.
	 * 
	 * @throws Exception
	 */
	void testChangeToDate() throws Exception;

	/**
	 * Test changing an empty field to a date field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToDate() throws Exception;

	/**
	 * Test changing to date list field.
	 * 
	 * @throws Exception
	 */
	void testChangeToDateList() throws Exception;

	/**
	 * Test changing an empty field to date list field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToDateList() throws Exception;

	/**
	 * Test changing to a html field.
	 * 
	 * @throws Exception
	 */
	void testChangeToHtml() throws Exception;

	/**
	 * Test changing an empty field to a html field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToHtml() throws Exception;

	/**
	 * Test changing to a html list field.
	 * 
	 * @throws Exception
	 */
	void testChangeToHtmlList() throws Exception;

	/**
	 * Test changing an empty field to a html list field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToHtmlList() throws Exception;

	/**
	 * Test changing to a micronode field.
	 * 
	 * @throws Exception
	 */
	void testChangeToMicronode() throws Exception;

	/**
	 * Test changing an empty field to a micronode field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToMicronode() throws Exception;

	/**
	 * Test changing to a micronode list field.
	 * 
	 * @throws Exception
	 */
	void testChangeToMicronodeList() throws Exception;

	/**
	 * Test changing an empty field to a micronode list field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToMicronodeList() throws Exception;

	/**
	 * Test changing to a node field.
	 * 
	 * @throws Exception
	 */
	void testChangeToNode() throws Exception;

	/**
	 * Test changing an empty field to a node field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToNode() throws Exception;

	/**
	 * Test changing to node list field.
	 * 
	 * @throws Exception
	 */
	void testChangeToNodeList() throws Exception;

	/**
	 * Test changing an empty field to a node list field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToNodeList() throws Exception;

	/**
	 * Test changing to a number field.
	 * 
	 * @throws Exception
	 */
	void testChangeToNumber() throws Exception;

	/**
	 * Test changing an empty field to a number field.
	 * 
	 * @throws Exception
	 */

	void testEmptyChangeToNumber() throws Exception;

	/**
	 * Test changing to a number list field.
	 * 
	 * @throws Exception
	 */
	void testChangeToNumberList() throws Exception;

	/**
	 * Test changing an empty field to a number list field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToNumberList() throws Exception;

	/**
	 * Test changing to string field.
	 * 
	 * @throws Exception
	 */
	void testChangeToString() throws Exception;

	/**
	 * Test changing an empty field to string field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToString() throws Exception;

	/**
	 * Test changing to string list field.
	 * 
	 * @throws Exception
	 */
	void testChangeToStringList() throws Exception;

	/**
	 * Test changing an empty field to string list field.
	 * 
	 * @throws Exception
	 */
	void testEmptyChangeToStringList() throws Exception;

}
