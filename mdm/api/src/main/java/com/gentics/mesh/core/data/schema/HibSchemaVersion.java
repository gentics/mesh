package com.gentics.mesh.core.data.schema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

public interface HibSchemaVersion extends HibFieldSchemaVersionElement<SchemaResponse, SchemaVersionModel, HibSchema, HibSchemaVersion> {

	// TODO MDM rename method
	HibSchema getSchemaContainer();

	// TODO MDM rename method
	void setSchemaContainer(HibSchema container);

	/**
	 * Transform the version to a reference POJO.
	 * 
	 * @return
	 */
	SchemaReference transformToReference();

	/**
	 * Return the element version of the schema. Please note that this is not the schema version. The element version instead reflects the update history of the
	 * element.
	 * 
	 * @return
	 */
	String getElementVersion();

	/**
	 * Return jobs which reference the schema version.
	 * 
	 * @return
	 */
	Iterable<? extends HibJob> referencedJobsViaTo();

	/**
	 * Check the autopurge flag of the version.
	 * 
	 * @return
	 */
	boolean isAutoPurgeEnabled();

	/**
	 * Get the hash over all uuids of microschema versions, which are currently assigned to the branch and which are used in the schema version.
	 * A microschema is "used" by a schema, if the schema contains a field of type "microschema", where the microschema is mentioned in the "allowed" property.
	 * @param branch branch
	 * @return hash
	 */
	default String getMicroschemaVersionHash(HibBranch branch) {
		return getMicroschemaVersionHash(branch, Collections.emptyMap());
	}

	/**
	 * Variant of {@link #getMicroschemaVersionHash(Branch)}, that gets a replacement map (which might be empty, but not null). The replacement map may map microschema names
	 * to microschema version uuids to be used instead of the currently assigned microschema version.
	 * @param branch branch
	 * @param replacementMap replacement map
	 * @return hash
	 */
	String getMicroschemaVersionHash(HibBranch branch, Map<String, String> replacementMap);

	/**
	 * Get the name of fields, using the microschema.
	 * A field uses the microschema, if it is either a of type "microschema" or of type "list of microschemas" and mentions the microschema in the "allowed" property.
	 * @param microschema microschema in question
	 * @return set of field names
	 */
	Set<String> getFieldsUsingMicroschema(HibMicroschema microschema);

	/**
	 * Check whether the schema uses the given microschema.
	 * A microschema is "used" by a schema, if the schema contains a field of type "microschema", where the microschema is mentioned in the "allowed" property.
	 * @param microschema microschema in question
	 * @return true, when the schema version uses the microschema, false if not
	 */
	default boolean usesMicroschema(HibMicroschema microschema) {
		return !getFieldsUsingMicroschema(microschema).isEmpty();
	}
}
