package com.gentics.mesh.parameter;

import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.MultiMap;

import java.util.Map;

import static com.gentics.mesh.util.HttpQueryUtils.toMap;

/**
 * Abstract class for parameter provider implementations.
 */
@GenerateDocumentation
public abstract class AbstractParameters implements ParameterProvider {

	protected MultiMap parameters;

	public AbstractParameters(ActionContext ac) {
		this(ac.getParameters());
		validate();
	}

	public AbstractParameters(MultiMap parameters) {
		this.parameters = parameters;
	}

	public AbstractParameters() {
		this(MultiMap.caseInsensitiveMultiMap());
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
		return toMap(parameters);
	}

	@Override
	public void setParameter(String name, String value) {
		parameters.set(name, value);
	}

	@Override
	public String toString() {
		return getQueryParameters();
	}

}
