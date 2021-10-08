package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.ElementType.SCHEMA;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for schema.
 */
public interface HibSchema extends HibFieldSchemaElement<SchemaResponse, SchemaVersionModel, HibSchema, HibSchemaVersion>, HibBucketableElement {

	TypeInfo TYPE_INFO = new TypeInfo(SCHEMA, SCHEMA_CREATED, SCHEMA_UPDATED, SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Transform the schema to a reference POJO.
	 * 
	 * @return
	 */
	default SchemaReference transformToReference() {
		return new SchemaReferenceImpl().setName(getName()).setUuid(getUuid());
	};

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/schemas/" + getUuid();
	}

	@Override
	default Map<HibBranch, HibSchemaVersion> findReferencedBranches() {
		Map<HibBranch, HibSchemaVersion> references = new HashMap<>();
		for (HibSchemaVersion version : Tx.get().schemaDao().findAllVersions(this)) {
			version.getBranches().forEach(branch -> references.put(branch, version));
		}
		return references;
	}

	default SchemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		String version = ac.getVersioningParameters().getVersion();
		// Delegate transform call to latest version
		if (version == null || version.equals("draft")) {
			return getLatestVersion().transformToRestSync(ac, level, languageTags);
		} else {
			HibSchemaVersion foundVersion = Tx.get().schemaDao().findVersionByRev(this, version);
			if (foundVersion == null) {
				throw error(NOT_FOUND, "object_not_found_for_uuid_version", getUuid(), version);
			}
			return foundVersion.transformToRestSync(ac, level, languageTags);
		}
	}
}
