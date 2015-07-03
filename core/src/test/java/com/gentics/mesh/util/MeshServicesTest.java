package com.gentics.mesh.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.test.AbstractDBTest;

public class MeshServicesTest extends AbstractDBTest {

	@Test
	public void testContainerInit() {
		assertNotNull(I18NService.getI18n());
		assertNotNull(MeshSpringConfiguration.getMeshSpringConfiguration());
	}

}
