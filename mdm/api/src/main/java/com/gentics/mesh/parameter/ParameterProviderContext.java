package com.gentics.mesh.parameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.impl.BackupParametersImpl;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.DisplayParametersImpl;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.ImageManipulationRetrievalParametersImpl;
import com.gentics.mesh.parameter.impl.IndexMaintenanceParametersImpl;
import com.gentics.mesh.parameter.impl.JobParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.ProjectPurgeParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.parameter.impl.UserParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;

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

	default ImageManipulationRetrievalParameters getImageManipulationRetrievalParameters() {
		return new ImageManipulationRetrievalParametersImpl(this);
	}

	default SearchParameters getSearchParameters() {
		return new SearchParametersImpl(this);
	}

	default BackupParameters getBackupParameters() {
		return new BackupParametersImpl(this);
	}

	default IndexMaintenanceParameters getIndexMaintenanceParameters() {
		return new IndexMaintenanceParametersImpl(this);
	}

	default JobParameters getJobParameters() {
		return new JobParametersImpl(this);
	}

	default DisplayParameters getDisplayParameters() {
		return new DisplayParametersImpl(this);
	}
}
