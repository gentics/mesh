package com.gentics.mesh.core.container;

public interface FieldDiffTestcases {

	/**
	 * Assert that no diff is detected when comparing two fields with the same value.
	 */
	void testNoDiffByValue();

	/**
	 * Assert that a diff is detected when comparing two fields with non-null values which are different from eachother.
	 */
	void testDiffByValue();

	/**
	 * Assert that no diff is detected when comparing two null value fields.
	 */
	void testNoDiffByValuesNull();

	/**
	 * Assert that a diff is detected when comparing a null field value with a non null field value.
	 */
	void testDiffByValueNonNull();

	/**
	 * Assert that a diff is detected when comparing a non-null field value with a different non-null field value.
	 */
	void testDiffByValueNonNull2();

	/**
	 * Assert that a diff is detected when comparing a non-null field with a container which does not contain this field.
	 */
	void testDiffBySchemaFieldRemoved();

	/**
	 * Assert that a diff is detected for an added field.
	 */
	void testDiffBySchemaFieldAdded();

	/**
	 * Assert that a diff is detected for a field type change for the two fields (one in containerA, another in B) with the same name.
	 */
	void testDiffBySchemaFieldTypeChanged();

}
