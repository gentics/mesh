package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import org.junit.Before;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

public abstract class AbstractTagFamilyQueryCountingTest extends AbstractCountingTest {
	public final static String PROJECT_NAME = "testproject";

	public final static int NUM_TAG_FAMILIES = 53;

	protected static int totalNumTagFamilies = NUM_TAG_FAMILIES;

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			// create project
			ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
			projectCreateRequest.setName(PROJECT_NAME);
			projectCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			call(() -> client().createProject(projectCreateRequest));

			// create tag families
			for (int i = 0; i < NUM_TAG_FAMILIES; i++) {
				createTagFamily(PROJECT_NAME, "tagfamily_%d".formatted(i));
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}
}
