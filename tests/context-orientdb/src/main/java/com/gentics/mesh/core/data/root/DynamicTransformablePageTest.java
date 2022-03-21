package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = false)
public class DynamicTransformablePageTest extends AbstractMeshTest {

	@Test
	public void testAddNode() {
		try (Tx tx = tx()) {
			UserRoot root = ((OrientDBBootstrapInitializer) boot()).meshRoot().getUserRoot();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(2, 2L);
			InternalActionContext ac = getMockedInternalActionContext("", user(), project());
			Page<?> page = new DynamicTransformablePageImpl<>(ac.getUser(), root, pagingInfo);
			long totalSize = page.getTotalElements();
			assertEquals(3, totalSize);
		}
	}

	@Test
	public void testEmptyPerPage() {
		try (Tx tx = tx()) {
			UserRoot root = ((OrientDBBootstrapInitializer) boot()).meshRoot().getUserRoot();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(2, null);
			InternalActionContext ac = getMockedInternalActionContext("", user(), project());
			Page<?> page = new DynamicTransformablePageImpl<>(ac.getUser(), root, pagingInfo);
			long totalSize = page.getTotalElements();
			assertEquals(1, page.getPageCount());
			assertNull(page.getPerPage());
			assertEquals(3, totalSize);
		}
	}

}
