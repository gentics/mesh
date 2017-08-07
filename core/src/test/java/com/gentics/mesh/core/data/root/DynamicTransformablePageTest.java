package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT_AND_NODE, startServer = false)
public class DynamicTransformablePageTest extends AbstractMeshTest {

	@Test
	public void testAddNode() {
		try (Tx tx = tx()) {
			NodeRoot root = boot().nodeRoot();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(2, 2);
			InternalActionContext ac = getMockedInternalActionContext("", user(), project());
			DynamicTransformablePageImpl page = new DynamicTransformablePageImpl<>(ac.getUser(), root, pagingInfo);
			long totalSize = page.getTotalElements();
			System.out.println(totalSize);
		}
	}

}
