package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.util.RestModelHelper;

import rx.Observable;

/**
 * @see SchemaContainer
 */
public class SchemaContainerImpl extends AbstractGraphFieldSchemaContainer<Schema, SchemaContainer, SchemaReference> implements SchemaContainer {

	@Override
	protected Class<? extends SchemaContainer> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	public static void checkIndices(Database database) {
		database.addVertexType(SchemaContainerImpl.class);
	}

	@Override
	protected String getMigrationAddress() {
		return NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS;
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
	public Observable<Schema> transformToRestSync(InternalActionContext ac, String... languageTags) {
		try {
			// Load the schema and add/overwrite some properties 
			Schema restSchema = JsonUtil.readSchema(getJson(), SchemaModel.class);
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
		Schema schema = ServerSchemaStorage.getInstance().getSchema(getName(), getVersion());
		if (schema == null) {
			try {
				schema = JsonUtil.readSchema(getJson(), SchemaModel.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			ServerSchemaStorage.getInstance().addSchema(schema);
		}
		return schema;

	}

	@Override
	public void setSchema(Schema schema) {
		ServerSchemaStorage.getInstance().removeSchema(schema.getName(), schema.getVersion());
		ServerSchemaStorage.getInstance().addSchema(schema);
		String json = JsonUtil.toJson(schema);
		setJson(json);
		setProperty(VERSION_PROPERTY_KEY, schema.getVersion());
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
	@Deprecated
	public Observable<? extends SchemaContainer> update(InternalActionContext ac) {
		throw new NotImplementedException("Updating is not directly supported for schemas. Please start a schema migration");
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		if (action == DELETE_ACTION) {
			// TODO Delete handling is not yet supported for schemas
		} else {
			String previousDocumentType = null;

			// TODO uncomment this code (in replacement for the following lines), as soon as getting previous schema containers is implemented
			//			SchemaContainer previousSchemaContainer = getPreviousVersion();
			//			if (previousSchemaContainer != null) {
			//				previousDocumentType = NodeIndexHandler.getDocumentType(previousSchemaContainer.getSchema());
			//			}
			int previousVersion = getVersion() - 1;
			if (previousVersion > 0) {
				previousDocumentType = getName() + "-" + previousVersion;
			}

			for (Node node : getNodes()) {
				batch.addEntry(node, UPDATE_ACTION);

				if (previousDocumentType != null) {
					List<String> languageNames = node.getAvailableLanguageNames();
					for (String languageTag : languageNames) {
						batch.addEntry(NodeIndexHandler.composeDocumentId(node, languageTag), node.getType(), DELETE_ACTION, previousDocumentType);
					}
				}
			}
		}
	}

}
