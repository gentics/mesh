package com.gentics.mesh.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Completable;

/**
 * Uniqueness test for tag names (per tagfamily)
 */
@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true)
public class TagNameUniquenessTest extends AbstractNameUniquenessTest {
	public final static String TAG_NAME = "testtag";

	@Override
	protected Completable createEntity(Optional<String> optParent) {
		return client().createTag(projectName(), optParent.get(), new TagCreateRequest().setName(TAG_NAME)).toCompletable();
	}

	@Override
	protected Optional<List<String>> parentEntities() {
		List<String> tagFamilies = Arrays.asList("first_tagfamily", "second_tagfamily", "third_tagfamily");
		List<String> tagFamilyUuids = new ArrayList<>();
		for (String tagFamily : tagFamilies) {
			tagFamilyUuids.add(client().createTagFamily(projectName(), new TagFamilyCreateRequest().setName(tagFamily)).blockingGet().getUuid());
		}

		return Optional.of(tagFamilyUuids);
	}
}
