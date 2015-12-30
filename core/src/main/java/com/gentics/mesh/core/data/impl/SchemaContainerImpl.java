package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.data.service.ServerSchemaStorage.getSchemaStorage;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.errorObservable;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestModelHelper;

import rx.Observable;

/**
 * @see SchemaContainer
 */
public class SchemaContainerImpl extends AbstractMeshCoreVertex<SchemaResponse, SchemaContainer> implements SchemaContainer {

	public static void checkIndices(Database database) {
		database.addVertexType(SchemaContainerImpl.class);
	}

	@Override
	public SchemaReference createEmptyReferenceModel() {
		return new SchemaReference();
	}

	@Override
	public String getType() {
		return SchemaContainer.TYPE;
	}

	@Override
	public Observable<SchemaResponse> transformToRest(InternalActionContext ac) {
		try {
			SchemaResponse restSchema = JsonUtil.readSchema(getJson(), SchemaResponse.class);
			restSchema.setUuid(getUuid());

			// TODO Get list of projects to which the schema was assigned
			// for (Project project : getProjects()) {
			// }
			// ProjectResponse restProject = new ProjectResponse();
			// restProje ct.setUuid(project.getUuid());
			// restProject.setName(project.getName());
			// schemaResponse.getProjects().add(restProject);
			// }

			// Sort the list by project name
			// restSchema.getProjects()
			// Collections.sort(restSchema.getProjects(), new Comparator<ProjectResponse>() {
			// @Override
			// public int compare(ProjectResponse o1, ProjectResponse o2) {
			// return o1.getName().compareTo(o2.getName());
			// };
			// });

			// Role permissions
			RestModelHelper.setRolePermissions(ac, this, restSchema);

			restSchema.setPermissions(ac.getUser().getPermissionNames(ac, this));

			return Observable.just(restSchema);
		} catch (IOException e) {
			return Observable.error(e);
		}
	}

	@Override
	public List<? extends Node> getNodes() {
		return in(HAS_SCHEMA_CONTAINER).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public void delete() {
		// TODO should all references be updated to a new fallback schema?
		addIndexBatch(DELETE_ACTION);
		getElement().remove();
	}

	@Override
	public SchemaContainerImpl getImpl() {
		return this;
	}

	private String getJson() {
		return getProperty("json");
	}

	private void setJson(String json) {
		setProperty("json", json);
	}

	@Override
	public Schema getSchema() {
		Schema schema = getSchemaStorage().getSchema(getName());
		if (schema == null) {
			try {
				schema = JsonUtil.readSchema(getJson(), SchemaImpl.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			getSchemaStorage().addSchema(schema);
		}
		return schema;

	}

	@Override
	public void setSchema(Schema schema) {
		getSchemaStorage().removeSchema(schema.getName());
		getSchemaStorage().addSchema(schema);
		String json = JsonUtil.toJson(schema);
		setJson(json);
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public Observable<? extends SchemaContainer> update(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		SchemaContainerRoot root = BootstrapInitializer.getBoot().meshRoot().getSchemaContainerRoot();

		try {
			SchemaUpdateRequest requestModel = JsonUtil.readSchema(ac.getBodyAsString(), SchemaUpdateRequest.class);
			if (StringUtils.isEmpty(requestModel.getName())) {
				return errorObservable(BAD_REQUEST, "error_name_must_be_set");
			}

			SchemaContainer foundSchema = root.findByName(requestModel.getName()).toBlocking().single();
			if (foundSchema != null && !foundSchema.getUuid().equals(getUuid())) {
				return errorObservable(BAD_REQUEST, "schema_conflicting_name", requestModel.getName());
			}

			return db.trx(() -> {
				if (!getName().equals(requestModel.getName())) {
					setName(requestModel.getName());
				}
				setSchema(requestModel);
				return addIndexBatch(UPDATE_ACTION);
			}).process().map(i -> this);
		} catch (Exception e) {
			return Observable.error(e);
		}

	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		if (action == DELETE_ACTION) {
			// TODO Delete handling is not yet supported for schemas
		} else {
			for (Node node : getNodes()) {
				batch.addEntry(node, UPDATE_ACTION);
			}
		}
	}

}
