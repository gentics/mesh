package com.gentics.mesh.parameter;

import java.util.Map;

import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.vertx.openapi.model.parameters.SimpleParameterProviderImpl;

import io.vertx.core.MultiMap;

/**
 * Abstract class for parameter provider implementations.
 */
@GenerateDocumentation
public abstract class AbstractParameters extends SimpleParameterProviderImpl {

	protected MultiMap parameters;

	public AbstractParameters(ActionContext ac) {
		this(ac.getParameters());
		validate();
	}

	public AbstractParameters(MultiMap parameters) {
		super(parameters);
	}

	public AbstractParameters() {
		this(MultiMap.caseInsensitiveMultiMap());
	}

	/**
	 * Return the RAML parameters for this provider.
	 * 
	 * @return
	 */
	@Override
	public abstract Map<? extends String, ? extends QueryParameter> getRAMLParameters();

	/**
	 * Returns the human readable name of the parameters.
	 *
	 * @return
	 */
	public abstract String getName();
}
