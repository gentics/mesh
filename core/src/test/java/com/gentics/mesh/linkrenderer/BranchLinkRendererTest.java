package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@RunWith(value = Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class BranchLinkRendererTest extends AbstractMeshTest {
	private WebRootLinkReplacer replacer;

	private String branchUuid;

	@Parameter(0)
	public String hostname;

	@Parameter(1)
	public String pathPrefix;

	@Parameter(2)
	public boolean ssl;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: hostname {0}, pathPrefix {1}, ssl {2}")
	public static Collection<Object[]> params() {
		Collection<Object[]> testData = new ArrayList<Object[]>();
		for (String hostname : Arrays.asList("www.nodea.com", "www.nodeb.com", null)) {
			for (String pathPrefix : Arrays.asList("", "/bla", "/bla/blubb", null)) {
				for (boolean ssl : Arrays.asList(true, false)) {
					testData.add(new Object[] { hostname, pathPrefix, ssl });
				}
			}
		}
		return testData;
	}

	@Before
	public void setupDeps() {
		// Grant admin perms. Otherwise we can't check the jobs
		tx(() -> group().addRole(roles().get("admin")));

		replacer = meshDagger().webRootLinkReplacer();

		String branchName = String.format("%s_%b", hostname, ssl);

		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);
		request.setHostname(hostname);
		request.setLatest(false);
		request.setSsl(ssl);
		request.setPathPrefix(pathPrefix);

		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Branch Response").isNotNull().hasName(branchName).isActive().isNotMigrated();
			branchUuid = response.getUuid();
		}, COMPLETED, 1);
	}

	@Test
	public void testResolve() {
		String prefix = "";
		if (!StringUtils.isEmpty(hostname)) {
			prefix = String.format("http%s://%s", ssl ? "s" : "", hostname);
		}
		if (!StringUtils.isEmpty(pathPrefix)) {
			prefix = prefix + pathPrefix;
		}

		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String uuid = newsNode.getUuid();
			final String content = "{{mesh.link('" + uuid + "')}}";
			InternalActionContext ac = mockActionContext();
			String replacedContent = replacer.replace(ac, branchUuid, ContainerType.DRAFT, content, LinkType.SHORT, null,
					null);

			assertEquals("Check rendered content", prefix + "/News/News%20Overview.en.html", replacedContent);
		}
	}
}
