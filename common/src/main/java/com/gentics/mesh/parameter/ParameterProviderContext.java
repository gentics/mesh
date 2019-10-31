package com.gentics.mesh.parameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.impl.*;

/**
 * Collection / Convenience interface which provides getters for parameter providers to all context implementations which use this interface.
 */
public interface ParameterProviderContext extends ActionContext {

	default NodeParameters getNodeParameters() {
		return new NodeParametersImpl(this);
	}

	default UserParameters getUserParameters() {
		return new UserParametersImpl(this);
	}

	default VersioningParameters getVersioningParameters() {
		return new VersioningParametersImpl(this);
	}

	default PagingParameters getPagingParameters() {
		return new PagingParametersImpl(this);
	}

	default RolePermissionParameters getRolePermissionParameters() {
		return new RolePermissionParametersImpl(this);
	}

	default ProjectPurgeParameters getProjectPurgeParameters() {
		return new ProjectPurgeParametersImpl(this);
	}

	default ImageManipulationParameters getImageParameters() {
		return new ImageManipulationParametersImpl(this);
	}

	default PublishParameters getPublishParameters() {
		return new PublishParametersImpl(this);
	}

	default DeleteParameters getDeleteParameters() {
		return new DeleteParametersImpl(this);
	}

	default SchemaUpdateParameters getSchemaUpdateParameters() {
		return new SchemaUpdateParametersImpl(this);
	}

	default GenericParameters getGenericParameters() {
		return new GenericParametersImpl(this);
	}

	default SearchParameters getSearchParameters() {
		return new SearchParametersImpl(this);
	}

	default GraphQLParameters getGraphQLParameters() {
		return new GraphQLParametersImpl(this);
	}
}
