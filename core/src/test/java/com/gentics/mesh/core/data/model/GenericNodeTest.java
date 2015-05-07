package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.service.generic.GenericNodeService;
import com.gentics.mesh.test.AbstractDBTest;

public class GenericNodeTest extends AbstractDBTest {

	@Autowired
	private GenericNodeService<GenericNode> nodeService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testFindByUUID() {

		Tag tag = data().getNews();
		GenericNode node = nodeService.findByUUID(tag.getUuid());
		assertNotNull(node);

	}
}
