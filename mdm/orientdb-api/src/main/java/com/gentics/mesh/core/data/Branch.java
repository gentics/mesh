package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;

/**
 * The Branch domain model interface.
 *
 * A branch is a bundle of specific schema versions which are used within a project. Branches can be used to create multiple tree structures within a single
 * project.
 * 
 * The branch will keep track of assigned versions and also store the information which schema version has ever been assigned to the branch.
 * 
 * A branch has the following responsibilities:
 * 
 * <ul>
 * <li>Manage assigned branches for the REST API</li>
 * <li>Provide information for node migration handlers. A handler must know what version needs to be migrated.</li>
 * <li>Provide information to the search index handler so that a list of indices can be compiled which should be used when searching</li>
 * <ul>
 * 
 * The latest version will be used for the creation of new nodes and should never be be downgraded. The other assigned versions will be used to manage
 * migrations and identify which branch specific search indices should be used when using the search indices.
 * 
 */
public interface Branch
	extends MeshCoreVertex<BranchResponse>, NamedElement, ReferenceableElement<BranchReference>, UserTrackingVertex, ProjectElement, HibBranch {

	static final String NAME = "name";

	static final String HOSTNAME = "hostname";

	static final String SSL = "ssl";

	static final String PATH_PREFIX = "pathPrefix";

	String UNIQUENAME_INDEX_NAME = "uniqueBranchNameIndex";

	/**
	 * Get the root vertex.
	 * 
	 * @return branch root to which the branch belongs
	 */
	BranchRoot getRoot();

	/**
	 * Find the branch schema edge for the given version.
	 *
	 * @param schemaVersion
	 * @return Found edge between branch and version
	 */
	HibBranchSchemaVersion findBranchSchemaEdge(HibSchemaVersion schemaVersion);

	/**
	 * Find the branch microschema edge for the given version.
	 *
	 * @param microschemaVersion
	 * @return Found edge between branch and version
	 */
	HibBranchMicroschemaVersion findBranchMicroschemaEdge(HibMicroschemaVersion microschemaVersion);
}
