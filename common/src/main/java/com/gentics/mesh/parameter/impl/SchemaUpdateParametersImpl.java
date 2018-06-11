package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.SchemaUpdateParameters;

public class SchemaUpdateParametersImpl extends AbstractParameters implements SchemaUpdateParameters {

	public SchemaUpdateParametersImpl(ActionContext ac) {
		super(ac);
	}

	public SchemaUpdateParametersImpl() {
		super();
	}

	@Override
	public void validate() {

	}
	
	@Override
	public String getName() {
		return "Schema update query parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter updateAssigned = new QueryParameter();
		updateAssigned.setDescription("Update the schema version for all branches which already utilize the schema.");
		updateAssigned.setDefaultValue("true");
		updateAssigned.setType(ParamType.BOOLEAN);
		updateAssigned.setDefaultValue("true");
		parameters.put(UPDATE_ASSIGNED_BRANCHES_QUERY_PARAM_KEY, updateAssigned);

		QueryParameter branchNames = new QueryParameter();
		branchNames.setDescription(
				"List of branch names which should be included in the update process. By default all branches which use the schema will be updated. You can thus use this parameter to only include a subset of branch in the update.");
		branchNames.setType(ParamType.STRING);
		branchNames.setExample("summerBranch,winterBranch");
		parameters.put(UPDATE_BRANCH_NAMES_QUERY_PARAM_KEY, branchNames);

		return parameters;
	}

}
