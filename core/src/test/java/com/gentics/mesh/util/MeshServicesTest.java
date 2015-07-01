package com.gentics.mesh.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.NodeService;
import com.gentics.mesh.core.data.service.MeshUserService;
import com.gentics.mesh.core.data.service.RoleService;
import com.gentics.mesh.core.data.service.SchemaContainerService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.test.AbstractDBTest;

public class MeshServicesTest extends AbstractDBTest {

	@Test
	public void testContainerInit() {
		assertNotNull(GroupService.getGroupService());
		assertNotNull(RoleService.getRoleService());
		assertNotNull(I18NService.getI18n());
		assertNotNull(LanguageService.getLanguageService());
		assertNotNull(NodeService.getNodeService());
		assertNotNull(SchemaContainerService.getSchemaService());
		assertNotNull(TagService.getTagService());
		assertNotNull(MeshUserService.getUserService());
		assertNotNull(MeshSpringConfiguration.getMeshSpringConfiguration());
	}

}
