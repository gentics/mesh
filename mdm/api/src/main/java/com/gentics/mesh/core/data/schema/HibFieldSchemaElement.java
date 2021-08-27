package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;

/**
 * Common interfaces shared by schema and microschema versions
 */
public interface HibFieldSchemaElement<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>>
	extends HibCoreElement<R>, HibUserTracking, HibNamedElement<R> {

	String getElementVersion();

	/**
	 * Return the latest container version.
	 * 
	 * @return Latest version
	 */
	SCV getLatestVersion();

	/**
	 * Set the latest container version.
	 * 
	 * @param version
	 */
	void setLatestVersion(SCV version);

	/**
	 * Return the version of the container using the version UUID as a reference.
	 * 
	 * @param uuid
	 * @return
	 */
	SCV findVersionByUuid(String uuid);

	/**
	 * Return the version of the container using the version revision as a reference.
	 * 
	 * @param version
	 * @return
	 */
	SCV findVersionByRev(String version);

	/**
	 * Return an iterable with all found schema versions.
	 * 
	 * @return
	 */
	Iterable<? extends SCV> findAll();
	/**
	 * Return a map of all branches which reference the container via an assigned container version. The found container version will be added as key to the
	 * map.
	 * 
	 * @return
	 */
	default Map<HibBranch, SCV> findReferencedBranches() {
		Map<HibBranch, SCV> references = new HashMap<>();
		for (SCV version : findAll()) {
			version.getBranches().forEach(branch -> references.put(branch, version));
		}
		return references;
	}

	@Override
	default R transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		String version = ac.getVersioningParameters().getVersion();
		// Delegate transform call to latest version
		if (version == null || version.equals("draft")) {
			return getLatestVersion().transformToRestSync(ac, level, languageTags);
		} else {
			SCV foundVersion = findVersionByRev(version);
			if (foundVersion == null) {
				throw error(NOT_FOUND, "object_not_found_for_uuid_version", getUuid(), version);
			}
			return foundVersion.transformToRestSync(ac, level, languageTags);
		}
	}
}
