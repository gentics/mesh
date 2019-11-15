package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.CREATOR_UUID_PROPERTY_KEY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.EDITOR_UUID_PROPERTY_KEY;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.User;

@Singleton
public class UserProperties {
	private final BootstrapInitializer boot;

	@Inject
	public UserProperties(BootstrapInitializer boot) {
		this.boot = boot;
	}

	public User getCreator(MeshVertex vertex) {
		return getUser(vertex, CREATOR_UUID_PROPERTY_KEY);
	}

	public User getEditor(MeshVertex vertex) {
		return getUser(vertex, EDITOR_UUID_PROPERTY_KEY);
	}

	public void setCreator(MeshVertex vertex, User user) {
		vertex.property(CREATOR_UUID_PROPERTY_KEY, user.getUuid());
	}

	public void setEditor(MeshVertex vertex, User user) {
		vertex.property(EDITOR_UUID_PROPERTY_KEY, user.getUuid());
	}

	private User getUser(MeshVertex vertex, String propertyKey) {
		return Optional.ofNullable(vertex)
			.map(v -> v.<String>getProperty(propertyKey))
			.map(boot.userRoot()::findByUuid)
			.orElse(null);
	}


}
