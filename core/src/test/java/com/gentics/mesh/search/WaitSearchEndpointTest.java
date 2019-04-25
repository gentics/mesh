package com.gentics.mesh.search;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.junit.Before;
import org.junit.Test;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;

@MeshTestSetting(elasticsearch = CONTAINER, testSize = FULL, startServer = true)
public class WaitSearchEndpointTest extends AbstractMeshTest {

	@Before
	public void setUp() throws Exception {
		createNode("slug", new StringFieldImpl().setString("waittest"));
	}

	private NodeListResponse search(boolean wait) {
		return client().searchNodes(
			getSimpleQuery("fields.slug", "waittest"),
			new SearchParametersImpl().setWait(wait)
		).blockingGet();
	}

	@Test
	public void searchWithWaitDisabled() {
		NodeListResponse result = search(false);

		assertThat(result.getMetainfo().getTotalCount()).isEqualTo(0);
	}

	@Test
	public void searchWithWaitEnabled() {
		NodeListResponse result = search(true);

		assertThat(result.getMetainfo().getTotalCount()).isEqualTo(1);
	}

}
