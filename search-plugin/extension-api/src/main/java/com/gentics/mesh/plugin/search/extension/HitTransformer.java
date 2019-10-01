package com.gentics.mesh.plugin.search.extension;

import com.gentics.mesh.core.rest.branch.BranchResponse;
import io.vertx.core.json.JsonObject;
import org.pf4j.ExtensionPoint;

/**
 * Interface for post-processing search results.
 *
 * <p>
 * The hit transformer gets the actual hit from the elastic search query, as
 * well as the GraqhQL query result loading further information about the found
 * page.
 * </p>
 *
 * <p>
 * Any information that should be included in the final result must be put into
 * the <code>output</code> object.
 * </p>
 */
public interface HitTransformer extends ExtensionPoint {

	/**
	 * Add additional fields to search output for a specific hit.
	 *
	 * @param rawHit The current hit of the elastic search query
	 * @param node Additional data of the found page loaded by GraphQL
	 * @param contextProject The project that received the search request
	 * @param branch The branch this hit was found in
	 * @return The actual JSON response for this particular hit
	 */
	JsonObject transform(JsonObject rawHit, JsonObject node, String contextProject, BranchResponse branch);
}
