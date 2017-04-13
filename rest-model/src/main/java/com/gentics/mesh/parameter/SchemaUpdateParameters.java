package com.gentics.mesh.parameter;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;

public interface SchemaUpdateParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY}
	 */
	public static final String UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY = "updateAssignedReleases";

	/**
	 * Query parameter key: {@value #UPDATE_RELEASE_NAMES_QUERY_PARAM_KEY}
	 */
	public static final String UPDATE_RELEASE_NAMES_QUERY_PARAM_KEY = "updateReleaseNames";

	/**
	 * Return the flag which indicates whether the created schema version should automatically be assigned to the releases which reference the schema.
	 * 
	 * @return
	 */
	default boolean getUpdateAssignedReleases() {
		String value = getParameter(UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY);
		return BooleanUtils.toBooleanDefaultIfNull(BooleanUtils.toBooleanObject(value), true);
	}

	/**
	 * Set the flag which is used to decide whether the schema version should be assigned to all releases which reference the schema.
	 * 
	 * @param flag
	 * @return
	 */
	default SchemaUpdateParameters setUpdateAssignedReleases(boolean flag) {
		setParameter(UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Get the names of the releases which should be updated once the new schema version has been created.
	 * 
	 * @return
	 */
	default List<String> getReleaseNames() {
		String value = getParameter(UPDATE_RELEASE_NAMES_QUERY_PARAM_KEY);
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
	 * Set the names of the releases which should be updated once the new schema version was created.
	 * 
	 * @param releaseNames
	 * @return Fluent API
	 */
	default SchemaUpdateParameters setReleaseNames(String... releaseNames) {
		setParameter(UPDATE_RELEASE_NAMES_QUERY_PARAM_KEY, convertToStr(releaseNames));
		return this;
	}

}
