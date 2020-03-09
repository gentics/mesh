package com.gentics.mesh.distributed.coordinator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.distributed.coordinator.proxy.RequestDelegatorImpl;

public class RequestDelegatorRegexTest {

	@Test
	public void testWhitelistRegex() {
		assertWhiteListed("/api/v1");
		assertWhiteListed("/api/v10");
		assertWhiteListed("/api/v1/admin/test");
		assertWhiteListed("/api/v1/admin/cluster/status");
	}

	@Test
	public void testReadOnlyRegex() {
		assertReadOnly("/api/v10/demo/graphql");
		assertReadOnly("/api/v1/demo/graphql");
		assertReadOnly("/api/v1/demo/graphql/");
		assertReadOnly("/api/v1/search");
		assertReadOnly("/api/v1/rawSearch");
		assertReadOnly("/api/v1/demo/search");
		assertReadOnly("/api/v1/demo/search/");
		assertReadOnly("/api/v1/demo/rawSearch");
		assertReadOnly("/api/v1/demo/rawSearch/");
		assertReadOnly("/api/v1/utilities/linkResolver");
		assertReadOnly("/api/v1/utilities/validateMicroschema");
	}

	private void assertWhiteListed(String path) {
		assertEquals("The path {" + path + "} is not whitelisted.", true, RequestDelegatorImpl.isWhitelisted(path));
	}

	private void assertReadOnly(String path) {
		assertEquals("The path {" + path + "} is not read only.", true, RequestDelegatorImpl.isReadOnly(path));
	}

}
