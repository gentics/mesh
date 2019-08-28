package com.gentics.mesh.parameter;

import org.apache.commons.lang.BooleanUtils;

import java.util.Arrays;
import java.util.List;

public interface SchemaUpdateParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY}
	 */
	String UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY = "updateAssignedBranches";

	/**
	 * Query parameter key: {@value #UPDATE_BRANCH_NAMES_QUERY_PARAM_KEY}
	 */
	String UPDATE_BRANCH_NAMES_QUERY_PARAM_KEY = "updateBranchNames";

	/**
	 * Query parameter key {@value #STRICT_VALIDATION_KEY}
	 */
	String STRICT_VALIDATION_KEY = "strictValidation";

	/**
	 * Return the flag which indicates whether the created schema version should automatically be assigned to the branches which reference the schema.
	 *
	 * @return
	 */
	default boolean getUpdateAssignedBranches() {
		String value = getParameter(UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY);
		return BooleanUtils.toBooleanDefaultIfNull(BooleanUtils.toBooleanObject(value), true);
	}

	/**
	 * Set the flag which is used to decide whether the schema version should be assigned to all branches which reference the schema.
	 *
	 * @param flag
	 * @return
	 */
	default SchemaUpdateParameters setUpdateAssignedBranches(boolean flag) {
		setParameter(UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Get the names of the branches which should be updated once the new schema version has been created.
	 *
	 * @return
	 */
	default List<String> getBranchNames() {
		String value = getParameter(UPDATE_BRANCH_NAMES_QUERY_PARAM_KEY);
		if (value == null) {
			return null;
		}
		String[] names = value.split(",");
		if (names == null) {
			return null;
		}
		return Arrays.asList(names);
	}

	/**
	 * Set the names of the branches which should be updated once the new schema version was created.
	 *
	 * @param branchNames
	 * @return Fluent API
	 */
	default SchemaUpdateParameters setBranchNames(String... branchNames) {
		setParameter(UPDATE_BRANCH_NAMES_QUERY_PARAM_KEY, convertToStr(branchNames));
		return this;
	}

	/**
	 * Set the strict validation flag which can be used to force search index validation.
	 *
	 * @param flag
	 * @return Fluent API
	 */
	default SchemaUpdateParameters setStrictValidation(boolean flag) {
		setParameter(STRICT_VALIDATION_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check whether the strict validation flag for search index validation is set.
	 *
	 * @return
	 */
	default boolean isStrictValidation() {
		return BooleanUtils.toBooleanDefaultIfNull(Boolean.valueOf(getParameter(STRICT_VALIDATION_KEY)), false);
	}

}
