package com.gentics.mesh.plugin.export.extension;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Interface to get ConfigValueProviders from plugins.
 * This interface has to be implemented to your regular {@link com.gentics.mesh.plugin.MeshPlugin Mesh-Plugin}.
 * Each Provider is getting called if the response has
 * not been cached yet; or when it got cleaned up from it.
 * The values are then used to map "CONFIG_VALUE" Mappings to
 * the correct values.
 *
 * @see com.gentics.mesh.plugin.MeshPlugin
 */
public interface ExportConfigValuePlugin {

	/**
	 * A list of ConfigValueProviders.
	 * You may not return `null`, and the List may not contain `null`!
	 *
	 * @return A list of the ConfigValueProviders the Plugin provides.
	 */
	@NotNull
	List<ConfigValueProvider> getConfigValueProviders();
}
