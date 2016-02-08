package com.gentics.mesh.core.schema.field;

import java.io.IOException;

/**
 * Interface for tests for field migrations
 */
public interface FieldMigrationTest {
	/**
	 * Test removing the field
	 * @throws IOException
	 */
	void testRemove() throws IOException;

	/**
	 * Test renaming the field
	 * @throws IOException
	 */
	void testRename() throws IOException;

	/**
	 * Test changing the field to a binary field
	 * @throws IOException
	 */
	void testChangeToBinary() throws IOException;

	/**
	 * Test changing the field to a boolean field
	 * @throws IOException
	 */
	void testChangeToBoolean() throws IOException;

	/**
	 * Test changing to a boolean list field
	 * @throws IOException
	 */
	void testChangeToBooleanList() throws IOException;

	/**
	 * Test changing to date field
	 * @throws IOException
	 */
	void testChangeToDate() throws IOException;

	/**
	 * Test changing to date list field
	 * @throws IOException
	 */
	void testChangeToDateList() throws IOException;

	/**
	 * Test changing to html field
	 * @throws IOException
	 */
	void testChangeToHtml() throws IOException;

	/**
	 * Test changing to html list field
	 * @throws IOException
	 */
	void testChangeToHtmlList() throws IOException;

	/**
	 * Test changing to micronode field
	 * @throws IOException
	 */
	void testChangeToMicronode() throws IOException;

	/**
	 * Test changing to micronode list field
	 * @throws IOException
	 */
	void testChangeToMicronodeList() throws IOException;

	/**
	 * Test changing to node field
	 * @throws IOException
	 */
	void testChangeToNode() throws IOException;

	/**
	 * Test changing to node list field
	 * @throws IOException
	 */
	void testChangeToNodeList() throws IOException;

	/**
	 * Test changing to number field
	 * @throws IOException
	 */
	void testChangeToNumber() throws IOException;

	/**
	 * Test changing to number list field
	 * @throws IOException
	 */
	void testChangeToNumberList() throws IOException;

	/**
	 * Test changing to string field
	 * @throws IOException
	 */
	void testChangeToString() throws IOException;

	/**
	 * Test changing to string list field
	 * @throws IOException
	 */
	void testChangeToStringList() throws IOException;
}
