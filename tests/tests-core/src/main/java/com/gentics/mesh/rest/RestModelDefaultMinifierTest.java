package com.gentics.mesh.rest;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.parameter.DisplayParameters;
import com.gentics.mesh.parameter.impl.DisplayParametersImpl;
import com.gentics.mesh.rest.client.impl.MeshOkHttpRequestImpl;
import com.gentics.mesh.test.MeshTestSetting;

import okhttp3.Response;

@MeshTestSetting(testSize = FULL, startServer = true)
public class RestModelDefaultMinifierTest extends RestModelTest {

	protected void testPayloadMinification(DisplayParameters... displayParameters) throws IOException {
		Response response = ((MeshOkHttpRequestImpl<ProjectListResponse>) client().findProjects(displayParameters)).getOkResponse().blockingGet();
		String responseBody = response.body().string();
		boolean expectMinified = Arrays.stream(displayParameters)
				.map(DisplayParameters::getMinify)
				.filter(Objects::nonNull).findAny()
				.orElseGet(() -> options().getHttpServerOptions().isMinifyJson());
		
		assertEquals("The payload is unexpectedly " + (expectMinified ? "not " : "") + "minified!: \n" + responseBody, expectMinified, responseBody.indexOf("\n") < 0);
	}

	@Test
	public void testPayloadDefaultMinification() throws IOException {
		testPayloadMinification();
	}

	@Test
	public void testPayloadForceMinify() throws IOException {
		testPayloadMinification(new DisplayParametersImpl().setMinify(true));
	}

	@Test
	public void testPayloadForcePretty() throws IOException {
		testPayloadMinification(new DisplayParametersImpl().setMinify(false));
	}
}
