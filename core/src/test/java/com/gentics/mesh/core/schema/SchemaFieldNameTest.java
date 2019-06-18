package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.fail;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.context.AbstractNaughtyStringTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaFieldNameTest extends AbstractNaughtyStringTest {

	private Predicate<String> isValidName = Pattern.compile("^[_A-Za-z][_0-9A-Za-z]*$").asPredicate();

	@Test
	public void testCreateSchema() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("test");
		request.addField(FieldUtil.createStringFieldSchema(input));
		runTest(client().createSchema(request));
	}

	@Test
	public void testCreateMicroschema() {
		MicroschemaCreateRequest request = new MicroschemaCreateRequest();
		request.setName("test");
		request.addField(FieldUtil.createStringFieldSchema(input));
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
