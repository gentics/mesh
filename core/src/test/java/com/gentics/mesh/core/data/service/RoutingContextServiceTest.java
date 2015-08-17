package com.gentics.mesh.core.data.service;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.test.AbstractDBTest;

public class RoutingContextServiceTest extends AbstractDBTest {

	@Autowired
	private RoutingContextService meshDatabaseService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

}
