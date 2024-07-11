package com.gentics.mesh.core.data.schema;

import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.data.NamedBaseElement;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.user.UserTracking;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;

/**
 * Common interfaces shared by schema and microschema versions
 */
public interface FieldSchemaElement<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends FieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends FieldSchemaVersionElement<R, RM, RE, SC, SCV>
	> extends CoreElement<R>, ReferenceableElement<RE>, UserTracking, NamedBaseElement {

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
	 * Return a map of all branches which reference the container via an assigned container version. The found container version will be added as key to the
	 * map.
	 * 
	 * @deprecated move this to DAO
	 * @return
	 */
	Map<Branch, SCV> findReferencedBranches();

	R transformToRestSync(InternalActionContext ac, int level, String... languageTags);
}
