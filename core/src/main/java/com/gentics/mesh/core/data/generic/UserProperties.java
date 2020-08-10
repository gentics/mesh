package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.CREATOR_UUID_PROPERTY_KEY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.EDITOR_UUID_PROPERTY_KEY;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.HibUser;
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
		setUser(vertex, user, CREATOR_UUID_PROPERTY_KEY);
	}

	public void setEditor(MeshVertex vertex, HibUser user) {
		setUser(vertex, user, EDITOR_UUID_PROPERTY_KEY);
	}

	private void setUser(MeshVertex vertex, HibUser user, String propertyKey) {
		if (user == null) {
			vertex.removeProperty(propertyKey);
		} else {
			vertex.property(propertyKey, user.getUuid());
		}
	}

	private User getUser(MeshVertex vertex, String propertyKey) {
		return Optional.ofNullable(vertex)
			.map(v -> v.<String>getProperty(propertyKey))
			.map(boot.userDao()::findByUuid)
			.orElse(null);
	}



}
