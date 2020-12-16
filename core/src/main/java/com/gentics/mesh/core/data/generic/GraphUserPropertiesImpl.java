package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.CREATOR_UUID_PROPERTY_KEY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.EDITOR_UUID_PROPERTY_KEY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.graphdb.model.MeshElement;

/**
 * Helper code that allows to resolve user references via the graph element properties.
 * 
 * @see UserProperties
 */
@Singleton
public class GraphUserPropertiesImpl implements UserProperties {

	private final BootstrapInitializer boot;

	@Inject
	public GraphUserPropertiesImpl(BootstrapInitializer boot) {
		this.boot = boot;
	}

	@Override
	public HibUser getCreator(HibBaseElement element) {
		return getUser(toGraph(element), CREATOR_UUID_PROPERTY_KEY);
	}

	@Override
	public HibUser getEditor(HibBaseElement element) {
		return getUser(toGraph(element), EDITOR_UUID_PROPERTY_KEY);
	}

	@Override
	public void setCreator(HibBaseElement element, HibUser user) {
		setUser(toGraph(element), user, CREATOR_UUID_PROPERTY_KEY);
	}

	@Override
	public void setEditor(HibBaseElement element, HibUser user) {
		setUser(toGraph(element), user, EDITOR_UUID_PROPERTY_KEY);
	}

	private void setUser(MeshElement element, HibUser user, String propertyKey) {
		if (user == null) {
			element.removeProperty(propertyKey);
		} else {
			element.property(propertyKey, user.getUuid());
		}
	}

	private HibUser getUser(MeshElement element, String propertyKey) {
		return Optional.ofNullable(element)
			.map(v -> v.<String>getProperty(propertyKey))
			.map(boot.userDao()::findByUuid)
			.orElse(null);
	}

}
