package com.gentics.mesh.parameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.UserParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;

/**
 * Collection / Convenience interface which provides getters for parameter providers to all context implementations which use this interface.
 */
public interface ParameterProviderContext extends ActionContext {

	default NodeParametersImpl getNodeParameters() {
		return new NodeParametersImpl(this);
	}

	default UserParameters getUserParameters() {
		return new UserParametersImpl(this);
	}

	default VersioningParametersImpl getVersioningParameters() {
		return new VersioningParametersImpl(this);
	}

	default PagingParametersImpl getPagingParameters() {
		return new PagingParametersImpl(this);
	}

	default RolePermissionParametersImpl getRolePermissionParameters() {
		return new RolePermissionParametersImpl(this);
	}

	default ImageManipulationParametersImpl getImageParameters() {
		return new ImageManipulationParametersImpl(this);
	}

	default PublishParametersImpl getPublishParameters() {
		return new PublishParametersImpl(this);
	}

	default SchemaUpdateParametersImpl getSchemaUpdateParameters() {
		return new SchemaUpdateParametersImpl(this);
	}

}
