package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.Events.JOB_WORKER_ADDRESS;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.SchemaUpdateParameters;
import com.gentics.mesh.util.Tuple;

/**
 * @see SchemaContainer
 */
public class SchemaContainerImpl extends
	AbstractGraphFieldSchemaContainer<SchemaResponse, SchemaModel, SchemaReference, SchemaContainer, SchemaContainerVersion> implements
	SchemaContainer {

	@Override
	protected Class<? extends SchemaContainer> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected Class<? extends SchemaContainerVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	public static void init(Database database) {
		database.addVertexType(SchemaContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaReference transformToReference() {
		return new SchemaReferenceImpl().setName(getName()).setUuid(getUuid());
	}

	@Override
	public List<? extends SchemaContainerRoot> getRoots() {
		return in(HAS_SCHEMA_CONTAINER_ITEM).toListExplicit(SchemaContainerRootImpl.class);
	}

	@Override
	public RootVertex<SchemaContainer> getRoot() {
		return MeshInternal.get().boot().meshRoot().getSchemaContainerRoot();
	}

	@Override
	public Iterable<? extends NodeImpl> getNodes() {
		return in(HAS_SCHEMA_CONTAINER).frameExplicit(NodeImpl.class);
	}

	@Override
	public void delete(BulkActionContext context) {
		// Check whether the schema is currently being referenced by nodes.
		Iterator<? extends NodeImpl> it = getNodes().iterator();
		if (!it.hasNext()) {
			context.batch().delete(this, true);
			for (SchemaContainerVersion v : findAll()) {
				v.delete(context);
			}
			remove();
		} else {
			throw error(BAD_REQUEST, "schema_delete_still_in_use", getUuid());
		}
	}

	@Override
	public boolean update(InternalActionContext ac, SearchQueueBatch batch) {
		SchemaUpdateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaUpdateRequest.class);

		SchemaContainerRootImpl.validateSchema(requestModel);

		// 2. Diff the schema with the latest version
		SchemaChangesListModel model = new SchemaChangesListModel();

		SchemaComparator comparator = MeshInternal.get().schemaComparator();
		model.getChanges().addAll(comparator.diff(getLatestVersion().getSchema(), requestModel));
		String schemaName = getName();

		// No changes -> done
		if (model.getChanges().isEmpty()) {
			message(ac, "schema_update_no_difference_detected", schemaName);
			return false;
		}

		SchemaUpdateParameters updateParams = ac.getSchemaUpdateParameters();
		User user = ac.getUser();
		Database db = MeshInternal.get().database();
		BootstrapInitializer boot = MeshInternal.get().boot();
		Tuple<SearchQueueBatch, String> info = db.tx(tx -> {

			// Check whether there are any microschemas which are referenced by the schema
			for (FieldSchema field : requestModel.getFields()) {
				if (field instanceof MicronodeFieldSchema) {
					MicronodeFieldSchema microschemaField = (MicronodeFieldSchema) field;

					String[] allowedSchemas = microschemaField.getAllowedMicroSchemas();
					if (allowedSchemas == null) {
						throw error(BAD_REQUEST, "schema_error_allowed_list_empty", microschemaField.getName());
					}

					// Check each allowed microschema individually
					for (String microschemaName : allowedSchemas) {

						// schema_error_microschema_reference_no_perm
						MicroschemaContainer microschema = boot.microschemaContainerRoot().findByName(microschemaName);
						if (microschema == null) {
							throw error(BAD_REQUEST, "schema_error_microschema_reference_not_found", microschemaName, field.getName());
						}
						if (!ac.getUser().hasPermission(microschema, READ_PERM)) {
							throw error(BAD_REQUEST, "schema_error_microschema_reference_no_perm", microschemaName, field.getName());
						}

						// Locate the projects to which the schema was linked - We need to ensure that the microschema is also linked to those projects
						for (SchemaContainerRoot roots : getRoots()) {
							Project project = roots.getProject();
							if (project != null) {
								project.getMicroschemaContainerRoot().addMicroschema(user, microschema);
							}
						}
					}
				}
			}

			// 3. Apply the found changes to the schema
			SchemaContainerVersion createdVersion = getLatestVersion().applyChanges(ac, model, batch);

			// Check whether the assigned branches of the schema should also directly be updated.
			// This will trigger a node migration.
			if (updateParams.getUpdateAssignedBranches()) {
				Map<Branch, SchemaContainerVersion> referencedBranches = findReferencedBranches();

				// Assign the created version to the found branches
				for (Map.Entry<Branch, SchemaContainerVersion> branchEntry : referencedBranches.entrySet()) {
					Branch branch = branchEntry.getKey();

					// Check whether a list of branch names was specified and skip branches which were not included in the list.
					List<String> branchNames = updateParams.getBranchNames();
					if (branchNames != null && !branchNames.isEmpty() && !branchNames.contains(branch.getName())) {
						continue;
					}

					// Assign the new version to the branch
					branch.assignSchemaVersion(user, createdVersion);
				}
			}
			return Tuple.tuple(batch, createdVersion.getVersion());
		});

		info.v1().processSync();
		GenericMessageResponse message = message(ac, "schema_updated_migration_deferred", schemaName, info.v2());
		if (updateParams.getUpdateAssignedBranches()) {
			Mesh.vertx().eventBus().send(JOB_WORKER_ADDRESS, null);
			message = message(ac, "schema_updated_migration_invoked", schemaName, info.v2());
		}

		ac.send(message, OK);
		return true;

	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/schemas/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
	}

}
