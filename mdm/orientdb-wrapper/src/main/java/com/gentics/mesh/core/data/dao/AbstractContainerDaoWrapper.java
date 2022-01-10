package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateMicroschemaChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateSchemaChangeImpl;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.result.Result;
import com.syncleus.ferma.FramedGraph;

import dagger.Lazy;

/**
 * A general wrapper implementation for {@link ContainerDao}.
 *
 * @author plyhun
 *
 * @param <R> contained entity type
 * @param <RM> contained entity version type
 * @param <RE> reference type
 * @param <SC> contained field element type
 * @param <SCV> contained field element version type
 * @param <M> container root entity type
 * @param <D> corresponding graphdb vertex type wrapper of R
 */
public abstract class AbstractContainerDaoWrapper<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
			M extends FieldSchemaContainer,
			D extends MeshCoreVertex<R>,
			DV extends MeshCoreVertex<R>> 
		extends AbstractCoreDaoWrapper<R, SC, D>
		implements PersistingContainerDao<R, RM, RE, SC, SCV, M> {

	public AbstractContainerDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@Override
	public void deleteChange(HibSchemaChange<? extends FieldSchemaContainer> change, BulkActionContext bac) {
		toGraph(change).delete(bac);
	}

	@Override
	public HibSchemaChange<?> createPersistedChange(SCV version, SchemaChangeOperation schemaChangeOperation) {
		FramedGraph graph = toGraph(version).getGraph();
		SchemaChange<?> schemaChange = null;
		switch (schemaChangeOperation) {
		case ADDFIELD:
			schemaChange = graph.addFramedVertex(AddFieldChangeImpl.class);
			break;
		case REMOVEFIELD:
			schemaChange = graph.addFramedVertex(RemoveFieldChangeImpl.class);
			break;
		case UPDATEFIELD:
			schemaChange = graph.addFramedVertex(UpdateFieldChangeImpl.class);
			break;
		case CHANGEFIELDTYPE:
			schemaChange = graph.addFramedVertex(FieldTypeChangeImpl.class);
			break;
		case UPDATESCHEMA:
			schemaChange = graph.addFramedVertex(UpdateSchemaChangeImpl.class);
			break;
		case UPDATEMICROSCHEMA:
			schemaChange = graph.addFramedVertex(UpdateMicroschemaChangeImpl.class);
			break;
		default:
			throw error(BAD_REQUEST, "error_change_operation_unknown", String.valueOf(schemaChangeOperation));
		}
		return schemaChange;
	}

	@Override
	public Result<? extends HibBranch> getBranches(SCV version) {
		return toGraph(version).getBranches();
	}

	@Override
	public SC createPersisted(String uuid) {
		SC vertex = super.createPersisted(uuid);
		vertex.setLatestVersion(createPersistedVersion(vertex));
		return vertex;
	}

	protected abstract RootVertex<D> getRoot();
}
