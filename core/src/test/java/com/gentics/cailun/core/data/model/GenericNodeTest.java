package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.test.AbstractDBTest;

public class GenericNodeTest extends AbstractDBTest {

	@Autowired
	private GenericNodeService<GenericNode> nodeService;

	@Before
	public void setup() {
		setupData();
	}

	@Test
	public void testFindByUUID() {

		Tag tag = data().getNews();
		GenericNode node = nodeService.findByUUID(tag.getUuid());
		assertNotNull(node);

	}
}
