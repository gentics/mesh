package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mda.ATx;
import com.gentics.mda.entitycollection.UserDao;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = false)
public class DynamicTransformablePageTest extends AbstractMeshTest {

	@Test
	public void testAddNode() {
		try (ATx tx = tx()) {
			UserDao root = tx.users();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(2, 2L);
			InternalActionContext ac = getMockedInternalActionContext("", user(), project());
			TransformablePage<?> page = new DynamicTransformablePageImpl<>(ac.getUser(), root.getDelegate(), pagingInfo);
			long totalSize = page.getTotalElements();
			assertEquals(3, totalSize);
		}
	}

	@Test
	public void testEmptyPerPage() {
		try (ATx tx = tx()) {
			UserDao root = tx.users();
			PagingParametersImpl pagingInfo = new PagingParametersImpl(2, null);
			InternalActionContext ac = getMockedInternalActionContext("", user(), project());
			TransformablePage<?> page = new DynamicTransformablePageImpl<>(ac.getUser(), root.getDelegate(), pagingInfo);
			long totalSize = page.getTotalElements();
			assertEquals(1, page.getPageCount());
			assertNull(page.getPerPage());
			assertEquals(3, totalSize);
		}
	}

}
