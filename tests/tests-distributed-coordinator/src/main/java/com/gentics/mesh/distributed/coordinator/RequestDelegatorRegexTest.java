package com.gentics.mesh.distributed.coordinator;

import static org.junit.Assert.assertEquals;

import com.gentics.mesh.distributed.DistributionUtils;
import org.junit.Test;

import com.gentics.mesh.distributed.coordinator.proxy.ClusterEnabledRequestDelegatorImpl;

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
		assertReadOnly("/api/v1/search/nodes");
		assertReadOnly("/api/v1/rawSearch/nodes");
		assertReadOnly("/api/v1/demo/search");
		assertReadOnly("/api/v1/demo/search/");
		assertReadOnly("/api/v1/demo/rawSearch");
		assertReadOnly("/api/v1/demo/rawSearch/");
		assertReadOnly("/api/v1/demo/search/nodes");
		assertReadOnly("/api/v1/demo/rawSearch/nodes");
		assertReadOnly("/api/v1/utilities/linkResolver");
		assertReadOnly("/api/v1/utilities/validateMicroschema");
		assertReadOnly("/api/v1/plugins/hello-world");
		assertReadOnly("/api/v1/some-project/plugins/hello-world");
	}

	@Test
	public void testBlacklist() {
		assertBlackListed("/api/v1/search/sync");
		assertBlackListed("/api/v1/search/clear");
		assertBlackListed("/api/v1/search/status");
	}

	private void assertWhiteListed(String path) {
		assertEquals("The path {" + path + "} is not whitelisted.", true, ClusterEnabledRequestDelegatorImpl.isWhitelisted(path));
	}

	private void assertReadOnly(String path) {
		assertEquals("The path {" + path + "} is not read only.", true, DistributionUtils.isReadOnly(path));
	}

	private void assertBlackListed(String path) {
		assertEquals("The path {" + path + "} is not blacklisted.", true, DistributionUtils.isBlackListed(path));
	}

}
