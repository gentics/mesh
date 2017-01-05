package com.gentics.mesh.core.group;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

public class GroupEndpointETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<GroupListResponse> response = client().findGroups().invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(client().findGroups(), etag, true);
			expectNo304(client().findGroups(new PagingParametersImpl().setPage(2)), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			Group group = group();

			MeshResponse<GroupResponse> response = client().findGroupByUuid(group.getUuid()).invoke();
			latchFor(response);
			String etag = group.getETag(getMockedInternalActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<GroupResponse> request = client().findGroupByUuid(group.getUuid());
			assertThat(expect304(request, etag, true)).contains(etag);

			// The node has no node reference and thus expanding will not affect the etag
			assertThat(expect304(client().findGroupByUuid(group.getUuid(), new NodeParameters().setExpandAll(true)), etag, true)).contains(etag);

			// Assert that adding bogus query parameters will not affect the etag
			expect304(client().findGroupByUuid(group.getUuid(), new NodeParameters().setExpandAll(false)), etag, true);
			expect304(client().findGroupByUuid(group.getUuid(), new NodeParameters().setExpandAll(true)), etag, true);
		}

	}

}
