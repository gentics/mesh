package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for microschema.
 */
public interface HibMicroschema
	extends HibFieldSchemaElement<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion>, HibBucketableElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.MICROSCHEMA, MICROSCHEMA_CREATED, MICROSCHEMA_UPDATED, MICROSCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return the latest version.
	 * 
	 * @return
	 */
	HibMicroschemaVersion getLatestVersion();

	/**
	 * Update the latest version reference.
	 * 
	 * @param version
	 */
	void setLatestVersion(HibMicroschemaVersion version);

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/microschemas/" + getUuid();
	}

	/**
	 * Return a map of all branches which reference the container via an assigned container version. The found container version will be added as key to the
	 * map.
	 * 
	 * @return
	 */
	default Map<HibBranch, HibMicroschemaVersion> findReferencedBranches() {
		Map<HibBranch, HibMicroschemaVersion> references = new HashMap<>();
		for (HibMicroschemaVersion version : Tx.get().microschemaDao().findAllVersions(this)) {
			version.getBranches().forEach(branch -> references.put(branch, version));
		}
		return references;
	}

	default MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		String version = ac.getVersioningParameters().getVersion();
		// Delegate transform call to latest version
		if (version == null || version.equals("draft")) {
			return getLatestVersion().transformToRestSync(ac, level, languageTags);
		} else {
			HibMicroschemaVersion foundVersion = Tx.get().microschemaDao().findVersionByRev(this, version);
			if (foundVersion == null) {
				throw error(NOT_FOUND, "object_not_found_for_uuid_version", getUuid(), version);
			}
			return foundVersion.transformToRestSync(ac, level, languageTags);
		}
	}

	/**
	 * Transform the microschema to a reference POJO.
	 * 
	 * @return
	 */
	default MicroschemaReference transformToReference() {
		return new MicroschemaReferenceImpl().setName(getName()).setUuid(getUuid());
	}

	/**
	 * Compose the document id for microschema index documents.
	 * 
	 * @param elementUuid
	 * @return
	 */
	static String composeDocumentId(String elementUuid) {
		Objects.requireNonNull(elementUuid, "A elementUuid must be provided.");
		return elementUuid;
	}

	/**
	 * Compose the index name for the microschema index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return "microschema";
	}
}
