package com.gentics.mesh.plugin.search.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.SearchParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.rest.client.MeshRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper functions to create Mesh requests.
 */
public class RequestUtils {

	private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);

	/**
	 * Create a <code>searchNodesRaw</code> request.
	 *
	 * <p>
	 *     The requests will be created with the {@link PluginContext#client() client} of the passed
	 *     <code>PluginContext</code>. When the <code>branch</code> parameter is present in the
	 *     <code>PluginContext</code>s {@link PluginContext#request() request}, it will be set
	 *     in the used {@link VersioningParameters}.
	 * </p>
	 *
	 * <p>
	 *     When <code>searchMultipleProjects</code> is <code>true</code> the global <code>rawSearch</code>
	 *     endpoint will be used, otherwise the project specific one.
	 * </p>
	 *
	 * @param pc The current plugin context
	 * @param query The Elasticsearch query to send
	 * @param searchMultipleProjects Whether multiple projects need to be searched
	 * @return The Mesh request for the specified query
	 */
	public static MeshRequest<ObjectNode> getSearchRequest(PluginContext pc, String query, boolean searchMultipleProjects, Boolean waitForIdle) {
		VersioningParameters version = new VersioningParametersImpl().published();
		SearchParameters search = new SearchParametersImpl();
		String branch = pc.queryParams().get("branch");

		if (StringUtils.isBlank(branch)) {
			log.trace("No branch specified");
		} else {
			log.debug("Adding branch parameter: {}", branch);
			version.setBranch(branch);
		}

		if (waitForIdle != null) {
			search.setWait(waitForIdle);
		}

		return searchMultipleProjects
			? pc.client().searchNodesRaw(query, version, search)
			: pc.client().searchNodesRaw(pc.projectName(), query, version, search);
	}
}
