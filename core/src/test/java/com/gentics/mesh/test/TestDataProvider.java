package com.gentics.mesh.test;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.error.MeshSchemaException;

public interface TestDataProvider {

	TagFamily getTagFamily(String key);

	Project getProject();

	Node getContent(String key);

	Node getFolder(String key);

	Map<String, User> getUsers();

	Map<String, Role> getRoles();

	Map<String, TagFamily> getTagFamilies();

	Tag getTag(String key);

	Map<String, ? extends Tag> getTags();

	UserInfo getUserInfo();

	SchemaContainer getSchemaContainer(String key);

	void setup() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException;

	Language getGerman();

	Language getEnglish();

	int getNodeCount();

	Map<String, SchemaContainer> getSchemaContainers();

	Map<String, Group> getGroups();

	Map<String, MicroschemaContainer> getMicroschemaContainers();

	MeshRoot getMeshRoot();

	UserInfo createUserInfo(String string, String string2, String string3);

	default Role role() {
		return getUserInfo().getRole();
	}

	default User user() {
		return getUserInfo().getUser();
	}

	default Group group() {
		return getUserInfo().getGroup();
	}
}
