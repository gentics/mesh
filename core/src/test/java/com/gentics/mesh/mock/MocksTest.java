package com.gentics.mesh.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.AbstractDBTest;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class MocksTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testMockParameters() throws Exception {

		String query = "lang=de,en";
		RoutingContext rc = Mocks.getMockedRoutingContext(query);
		assertEquals("The query did not match up.", query, rc.request().query());
		MultiMap params = rc.request().params();
		assertNotNull("The routing context request parameters were null.", params);
		assertTrue("The routing context request parameters did not contain the lang parameter. Size {" + params.size() + "}",
				params.contains("lang"));

		try (NoTx noTx = db.noTx()) {
			InternalActionContext ac = Mocks.getMockedInternalActionContext("lang=de,en");
			assertThat(ac.getNodeParameters().getLanguages()).containsExactly("de", "en");
		}
	}
}
