package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.error.GenericRestException;

public class AbstractHandler {

	/**
	 * Assert that the parameter has been specified. A {@link GenericRestException} will be thrown if the parameter was not specified.
	 * 
	 * @param value
	 *            Parameter value
	 * @param name
	 *            Parameter name
	 */
	protected void validateParameter(String value, String name) {
		if (StringUtils.isEmpty(value)) {
			throw error(BAD_REQUEST, "error_request_parameter_missing", name);
		}
	}

}
