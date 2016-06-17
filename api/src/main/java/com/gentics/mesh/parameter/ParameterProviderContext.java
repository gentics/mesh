package com.gentics.mesh.parameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.parameter.impl.TakeOfflineParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;

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

	default TakeOfflineParameters getTakeOfflineParameters() {
		return new TakeOfflineParameters(this);
	}

}
