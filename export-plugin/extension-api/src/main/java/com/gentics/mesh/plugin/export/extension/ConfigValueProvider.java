package com.gentics.mesh.plugin.export.extension;

import io.reactivex.Single;
import org.pf4j.ExtensionPoint;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * A Provider for configuration mappings which are used to map `CONFIG_VALUE`
 * mapping definitions to their respective value.
 */
public interface ConfigValueProvider extends ExtensionPoint {

	/**
	 * Method to generate the required Config-Values for the export.
	 * The response is getting cached automatically and will be called
	 * once the cache has timed out for this specific Provider.
	 *
	 * @param projectName The Project-Name for which this mapping should be loaded for.
	 *
	 * @return A single which resolves the config values.
	 */
	@NotNull
	Single<Map<String, Map<String, String>>> getConfigValues(String projectName);
}
