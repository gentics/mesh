package com.gentics.mesh.parameter.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;

public class SchemaUpdateParameters extends AbstractParameters {

	/**
	 * Query parameter key: {@value #UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY}
	 */
	public static final String UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY = "updateAssignedReleases";

	/**
	 * Query parameter key: {@value #UPDATE_RELEASE_NAMES_QUERY_PARAM_KEY}
	 */
	public static final String UPDATE_RELEASE_NAMES_QUERY_PARAM_KEY = "updateReleaseNames";

	public SchemaUpdateParameters(ActionContext ac) {
		super(ac);
	}

	public SchemaUpdateParameters() {
		super();
	}

	@Override
	public void validate() {

	}

	/**
	 * Return the flag which indicates whether the created schema version should automatically be assigned to the releases which reference the schema.
	 * 
	 * @return
	 */
	public boolean getUpdateAssignedReleases() {
		String value = getParameter(UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY);
		return BooleanUtils.toBooleanDefaultIfNull(BooleanUtils.toBooleanObject(value), true);
	}

	/**
	 * Set the flag which is used to decide whether the schema version should be assigned to all releases which reference the schema.
	 * 
	 * @param flag
	 * @return
	 */
	public SchemaUpdateParameters setUpdateAssignedReleases(boolean flag) {
		setParameter(UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Get the names of the releases which should be updated once the new schema version has been created.
	 * 
	 * @return
	 */
	public List<String> getReleaseNames() {
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
	public SchemaUpdateParameters setReleaseNames(String... releaseNames) {
		setParameter(UPDATE_RELEASE_NAMES_QUERY_PARAM_KEY, convertToStr(releaseNames));
		return this;
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter updateAssigned = new QueryParameter();
		updateAssigned.setDescription("Update the schema version for all releases which already utilize the schema.");
		updateAssigned.setDefaultValue("true");
		updateAssigned.setType(ParamType.BOOLEAN);
		updateAssigned.setDefaultValue("true");
		parameters.put(UPDATE_ASSIGNED_RELEASES_QUERY_PARAM_KEY, updateAssigned);

		QueryParameter releaseNames = new QueryParameter();
		releaseNames.setDescription(
				"List of release names which should be included in the update process. By default all releases which use the schema will be updated. You can thus use this parameter to only include a subset of release in the update.");
		releaseNames.setType(ParamType.STRING);
		releaseNames.setExample("summerRelease,winterRelease");
		parameters.put(UPDATE_RELEASE_NAMES_QUERY_PARAM_KEY, releaseNames);

		return parameters;
	}

}
