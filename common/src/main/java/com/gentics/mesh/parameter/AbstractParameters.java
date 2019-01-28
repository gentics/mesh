package com.gentics.mesh.parameter;

import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.handler.ActionContext;

import java.util.HashMap;
import java.util.Map;

import static com.gentics.mesh.util.HttpQueryUtils.toMap;

/**
 * Abstract class for parameter provider implementations.
 */
@GenerateDocumentation
public abstract class AbstractParameters implements ParameterProvider {

	protected Map<String, String> parameters;

	public AbstractParameters(ActionContext ac) {
		this(toMap(ac.getParameters()));
		validate();
	}

	public AbstractParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public AbstractParameters() {
		this(new HashMap<>());
	}

	/**
	 * Returns the human readable name of the parameters.
	 * 
	 * @return
	 */
	public abstract String getName();

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public void setParameter(String name, String value) {
		parameters.put(name, value);
	}

	@Override
	public String toString() {
		return getQueryParameters();
	}

}
