package com.gentics.mesh.core.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.search.AbstractSearchVerticleTest;

import io.vertx.core.Future;

public class SearchVerticleTest extends AbstractSearchVerticleTest {

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		return list;
	}

	@Test
	public void testLoadSearchStatus() {
		Future<SearchStatusResponse> future = getClient().loadSearchStatus();
		latchFor(future);
		assertSuccess(future);
		SearchStatusResponse status = future.result();
		assertNotNull(status);
		assertEquals(0, status.getBatchCount());
	}

}
