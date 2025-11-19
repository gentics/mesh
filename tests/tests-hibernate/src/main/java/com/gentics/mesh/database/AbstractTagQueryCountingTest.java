package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import org.junit.Before;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;

public abstract class AbstractTagQueryCountingTest extends AbstractCountingTest {
	public final static String PROJECT_NAME = "testproject";

	public final static String TAGFAMILY_NAME = "tagfamily";

	public final static int NUM_TAGS = 53;

	protected static int totalNumTags = NUM_TAGS;

	protected static String tagFamilyUuid;

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			// create project
			ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
			projectCreateRequest.setName(PROJECT_NAME);
			projectCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			call(() -> client().createProject(projectCreateRequest));

			TagFamilyResponse tagFamily = createTagFamily(PROJECT_NAME, TAGFAMILY_NAME);
			tagFamilyUuid = tagFamily.getUuid();

			// create tags
			for (int i = 0; i < NUM_TAGS; i++) {
				createTag(PROJECT_NAME, tagFamilyUuid, "tag_%d".formatted(i));
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}
}
