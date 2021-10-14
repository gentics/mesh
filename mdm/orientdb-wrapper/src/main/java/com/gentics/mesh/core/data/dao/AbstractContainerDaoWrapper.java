package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;

import dagger.Lazy;

public abstract class AbstractContainerDaoWrapper<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
			M extends FieldSchemaContainer,
			D extends MeshCoreVertex<R>> 
		extends AbstractCoreDaoWrapper<R, SC, D>
		implements ContainerDao<R, RM, RE, SC, SCV, M> {

	public AbstractContainerDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot,
			Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	@Override
	public void deleteChange(HibSchemaChange<?> change, BulkActionContext bac) {
		toGraph(change).delete(bac);
	}
}
