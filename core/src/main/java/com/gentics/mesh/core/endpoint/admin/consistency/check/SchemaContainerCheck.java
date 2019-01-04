package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Schema container specific checks.
 */
public class SchemaContainerCheck implements ConsistencyCheck {

	@Override
	public void invoke(LegacyDatabase db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends SchemaContainer> it = db.getVerticesForType(SchemaContainerImpl.class);
		while (it.hasNext()) {
			checkSchemaContainer(it.next(), response);
		}

		Iterator<? extends SchemaContainerVersionImpl> vIt = db.getVerticesForType(SchemaContainerVersionImpl.class);
		while (vIt.hasNext()) {
			checkSchemaContainerVersion(vIt.next(), response);
		}
	}

	private void checkSchemaContainer(SchemaContainer schemaContainer, ConsistencyCheckResponse response) {
		String uuid = schemaContainer.getUuid();

		checkIn(schemaContainer, HAS_SCHEMA_CONTAINER_ITEM, SchemaContainerRootImpl.class, response, HIGH);
		checkOut(schemaContainer, HAS_LATEST_VERSION, SchemaContainerVersionImpl.class, response, HIGH);

		// checkOut(schemaContainer, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(schemaContainer, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(schemaContainer.getName())) {
			response.addInconsistency("Schema container name is empty or null", uuid, HIGH);
		}
		if (schemaContainer.getCreationTimestamp() == null) {
			response.addInconsistency("The schemaContainer creation date is not set", uuid, MEDIUM);
		}
		if (schemaContainer.getLastEditedTimestamp() == null) {
			response.addInconsistency("The schemaContainer edit timestamp is not set", uuid, MEDIUM);
		}
	}

	private void checkSchemaContainerVersion(SchemaContainerVersion schemaContainerVersion, ConsistencyCheckResponse response) {
		String uuid = schemaContainerVersion.getUuid();

		checkIn(schemaContainerVersion, HAS_PARENT_CONTAINER, SchemaContainerImpl.class, response, HIGH);

		if (isEmpty(schemaContainerVersion.getName())) {
			response.addInconsistency("Schema container Version name is empty or null", uuid, MEDIUM);
		}
		if (isEmpty(schemaContainerVersion.getVersion())) {
			response.addInconsistency("Schema container version number is empty or null", uuid, MEDIUM);
		} else {
			try {
				Double version = Double.valueOf(schemaContainerVersion.getVersion());
				if (version > 1) {
					SchemaContainerVersion previousVersion = schemaContainerVersion.getPreviousVersion();
					if (previousVersion == null) {
						response.addInconsistency(String.format("Schema container version %s must have a previous version", schemaContainerVersion
								.getVersion()), uuid, MEDIUM);
					} else {
						String expectedPrevious = String.valueOf(version - 1);
						if (!expectedPrevious.equals(previousVersion.getVersion())) {
							response.addInconsistency(String.format("Previous schema container version must have number %s but has %s",
									expectedPrevious, previousVersion.getVersion()), uuid, MEDIUM);
						}

						MeshVertex parent = in(HAS_PARENT_CONTAINER, SchemaContainerImpl.class).follow(schemaContainerVersion);
						if (parent != null) {
							MeshVertex previousParent = in(HAS_PARENT_CONTAINER, SchemaContainerImpl.class).follow(previousVersion);
							if (!parent.equals(previousParent)) {
								response.addInconsistency("Previous schema container version has different schema container", uuid, HIGH);
							}
						}
					}
				}
			} catch (NumberFormatException e) {
				response.addInconsistency(String.format("Schema container version number %s is empty or null", schemaContainerVersion.getVersion()),
						uuid, MEDIUM);
			}
		}
	}
}
