package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_ITEM;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Schema container specific checks.
 */
public class SchemaContainerCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "schemas";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult a = processForType(db, SchemaContainerImpl.class, (schema, result) -> {
			checkSchemaContainer(schema, result);
		}, attemptRepair, tx);

		ConsistencyCheckResult b = processForType(db, SchemaContainerVersionImpl.class, (version, result) -> {
			checkSchemaContainerVersion(version, result);
		}, attemptRepair, tx);

		return a.merge(b);
	}

	private void checkSchemaContainer(SchemaContainer schemaContainer, ConsistencyCheckResult result) {
		String uuid = schemaContainer.getUuid();

		checkIn(schemaContainer, HAS_SCHEMA_CONTAINER_ITEM, SchemaContainerRootImpl.class, result, HIGH);
		checkOut(schemaContainer, HAS_LATEST_VERSION, SchemaContainerVersionImpl.class, result, HIGH);

		// checkOut(schemaContainer, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(schemaContainer, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(schemaContainer.getName())) {
			result.addInconsistency("Schema container name is empty or null", uuid, HIGH);
		}
		if (schemaContainer.getCreationTimestamp() == null) {
			result.addInconsistency("The schemaContainer creation date is not set", uuid, MEDIUM);
		}
		if (schemaContainer.getLastEditedTimestamp() == null) {
			result.addInconsistency("The schemaContainer edit timestamp is not set", uuid, MEDIUM);
		}
	}

	private void checkSchemaContainerVersion(SchemaContainerVersion schemaContainerVersion, ConsistencyCheckResult result) {
		String uuid = schemaContainerVersion.getUuid();

		checkIn(schemaContainerVersion, HAS_PARENT_CONTAINER, SchemaContainerImpl.class, result, HIGH);

		if (isEmpty(schemaContainerVersion.getName())) {
			result.addInconsistency("Schema container Version name is empty or null", uuid, MEDIUM);
		}
		if (isEmpty(schemaContainerVersion.getVersion())) {
			result.addInconsistency("Schema container version number is empty or null", uuid, MEDIUM);
		} else {
			try {
				Double version = Double.valueOf(schemaContainerVersion.getVersion());
				if (version > 1) {
					SchemaContainerVersion previousVersion = schemaContainerVersion.getPreviousVersion();
					if (previousVersion == null) {
						result.addInconsistency(String.format("Schema container version %s must have a previous version", schemaContainerVersion
							.getVersion()), uuid, MEDIUM);
					} else {
						String expectedPrevious = String.valueOf(version - 1);
						if (!expectedPrevious.equals(previousVersion.getVersion())) {
							result.addInconsistency(String.format("Previous schema container version must have number %s but has %s",
								expectedPrevious, previousVersion.getVersion()), uuid, MEDIUM);
						}

						MeshVertex parent = in(HAS_PARENT_CONTAINER, SchemaContainerImpl.class).follow(schemaContainerVersion);
						if (parent != null) {
							MeshVertex previousParent = in(HAS_PARENT_CONTAINER, SchemaContainerImpl.class).follow(previousVersion);
							if (!parent.equals(previousParent)) {
								result.addInconsistency("Previous schema container version has different schema container", uuid, HIGH);
							}
						}
					}
				}
			} catch (NumberFormatException e) {
				result.addInconsistency(String.format("Schema container version number %s is empty or null", schemaContainerVersion.getVersion()),
					uuid, MEDIUM);
			}
		}
	}
}
