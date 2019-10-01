package com.gentics.mesh.plugin.comment;

import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import io.reactivex.Observable;
import org.junit.Test;

import static com.gentics.mesh.plugin.comment.testclient.TestClientUtil.createSchemaResponse;

public class SchemaInitializerTest extends AbstractPluginTest {

	@Test
	public void testPrefixes() {
		component.schemaInitializer().initialize().subscribe();

		assertSchemasAdded("comment_comment", "comment_section", "folder");
	}

	@Test
	public void testAlreadyExisting() {
		addSchema("comment_comment");
		addSchema("comment_section");
		addSchema("folder");

		component.schemaInitializer().initialize().subscribe();

		assertSchemasAdded();
	}

	private void addSchema(String name) {
		adminClient.getSchemas().add(createSchemaResponse(name));
	}

	private void assertSchemasAdded(String... schemaNames) {
		Observable.fromIterable(component.adminClient().getCreatedSchemas())
			.map(SchemaCreateRequest::getName)
			.test()
			.assertResult(schemaNames);
	}
}
