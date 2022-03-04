package com.gentics.mesh.core.schema;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractNaughtyStringTest;

import org.junit.Test;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.fail;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaNameTest extends AbstractNaughtyStringTest {
	private Predicate<String> isValidName = Pattern.compile("^[_A-Za-z][_0-9A-Za-z]*$").asPredicate();

	@Test
	public void testCreateSchema() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(input);
		runTest(client().createSchema(request));
	}

	@Test
	public void testCreateMicroschema() {
		MicroschemaCreateRequest request = new MicroschemaCreateRequest();
		request.setName(input);
		runTest(client().createMicroschema(request));
	}

	private void runTest(MeshRequest<?> request) {
		if (isValidName.test(input)) {
			request.blockingAwait();
		} else {
			try {
				request.blockingAwait();
				fail("Request should have thrown an exception. Tested string: " + input);
			} catch (RuntimeException ex) {
				call(() -> request, BAD_REQUEST);
			}
		}
	}
}
