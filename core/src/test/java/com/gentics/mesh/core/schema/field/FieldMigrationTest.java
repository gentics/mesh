package com.gentics.mesh.core.schema.field;

/**
 * Interface for tests for field migrations
 */
public interface FieldMigrationTest {
	/**
	 * Test removing the field
	 */
	void testRemove();

	/**
	 * Test renaming the field
	 */
	void testRename();

	/**
	 * Test changing the field to a binary field
	 */
	void testChangeToBinary();

	/**
	 * Test changing the field to a boolean field
	 */
	void testChangeToBoolean();

	/**
	 * Test changing to a boolean list field
	 */
	void testChangeToBooleanList();

	/**
	 * Test changing to date field
	 */
	void testChangeToDate();

	/**
	 * Test changing to date list field
	 */
	void testChangeToDateList();

	/**
	 * Test changing to html field
	 */
	void testChangeToHtml();

	/**
	 * Test changing to html list field
	 */
	void testChangeToHtmlList();

	/**
	 * Test changing to micronode field
	 */
	void testChangeToMicronode();

	/**
	 * Test changing to micronode list field
	 */
	void testChangeToMicronodeList();

	/**
	 * Test changing to node field
	 */
	void testChangeToNode();

	/**
	 * Test changing to node list field
	 */
	void testChangeToNodeList();

	/**
	 * Test changing to number field
	 */
	void testChangeToNumber();

	/**
	 * Test changing to number list field
	 */
	void testChangeToNumberList();

	/**
	 * Test changing to string field
	 */
	void testChangeToString();

	/**
	 * Test changing to string list field
	 */
	void testChangeToStringList();

	/**
	 * Test migrating the field with a custom migration script
	 */
	void testCustomMigrationScript();

	/**
	 * Test with an invalid migration script
	 */
	void testInvalidMigrationScript();
}
