package com.gentics.mesh.parameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.parameter.impl.SchemaUpdateParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;

/**
 * Collection / Convenience interface which provides getters for parameter providers to all context implementations which use this interface.
 */
public interface ParameterProviderContext extends ActionContext {

	default NodeParameters getNodeParameters() {
		return new NodeParameters(this);
	}

	default VersioningParameters getVersioningParameters() {
		return new VersioningParameters(this);
	}

	default PagingParameters getPagingParameters() {
		return new PagingParameters(this);
	}

	default RolePermissionParameters getRolePermissionParameters() {
		return new RolePermissionParameters(this);
	}

	default ImageManipulationParameters getImageParameters() {
		return new ImageManipulationParameters(this);
	}

	default PublishParameters getPublishParameters() {
		return new PublishParameters(this);
	}

	default SchemaUpdateParameters getSchemaUpdateParameters() {
		return new SchemaUpdateParameters(this);
	}

}
