package com.gentics.mesh.mock;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(elasticsearch = NONE, testSize = PROJECT, startServer = false)
public class MocksTest extends AbstractMeshTest {

	@Test
	public void testMockParameters() throws Exception {
		try (Tx tx = tx()) {
			String query = "lang=de,en";
			RoutingContext rc = mockRoutingContext(query);
			assertEquals("The query did not match up.", query, rc.request().query());
			MultiMap params = rc.request().params();
			assertNotNull("The routing context request parameters were null.", params);
			assertTrue("The routing context request parameters did not contain the lang parameter. Size {" + params.size() + "}",
					params.contains("lang"));

			InternalActionContext ac = mockActionContext("lang=de,en");
			assertThat(ac.getNodeParameters().getLanguages()).containsExactly("de", "en");
		}
	}
}
