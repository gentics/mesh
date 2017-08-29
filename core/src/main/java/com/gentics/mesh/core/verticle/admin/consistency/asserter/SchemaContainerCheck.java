package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;

/**
 * Schema container specific checks.
 */
public class SchemaContainerCheck implements ConsistencyCheck {

	@Override
	public void invoke(BootstrapInitializer boot, ConsistencyCheckResponse response) {
		for (SchemaContainer schemaContainer : boot.schemaContainerRoot().findAll()) {
			checkSchemaContainer(schemaContainer, response);
		}
	}

	private void checkSchemaContainer(SchemaContainer schemaContainer, ConsistencyCheckResponse response) {
		String uuid = schemaContainer.getUuid();

		if (isEmpty(schemaContainer.getName())) {
			response.addInconsistency("Schema container name is empty or null", uuid, HIGH);
		}
		if (schemaContainer.getCreationTimestamp() == null) {
			response.addInconsistency("The schemaContainer creation date is not set", uuid, MEDIUM);
		}
		if (schemaContainer.getCreator() == null) {
			response.addInconsistency("The schemaContainer creator is not set", uuid, MEDIUM);
		}
		if (schemaContainer.getEditor() == null) {
			response.addInconsistency("The schemaContainer editor is not set", uuid, MEDIUM);
		}
		if (schemaContainer.getLastEditedTimestamp() == null) {
			response.addInconsistency("The schemaContainer edit timestamp is not set", uuid, MEDIUM);
		}

		SchemaContainerVersion latestVersion = schemaContainer.getLatestVersion();
		if (schemaContainer.getLatestVersion() == null) {
			response.addInconsistency("Schema container has no latest version", uuid, HIGH);
		} else {
			if (latestVersion.getVersion() == null) {
				response.addInconsistency("Schema container version {" + latestVersion.getUuid() + "} of schema has no version property", uuid, HIGH);
			}
		}

	}

}
