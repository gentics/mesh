package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.tag.TagListResponse;

public class TagListResponseAssert extends AbstractAssert<TagListResponseAssert, TagListResponse> {

	public TagListResponseAssert(TagListResponse actual) {
		super(actual, TagListResponseAssert.class);
	}

	/**
	 * Assert that the tag contains exactly the given set of tags by name.
	 * 
	 * @param tagNames
	 */
	public void containsExactly(String... tagNames) {
		List<String> tags = actual.getData().stream().map(t -> t.getName()).collect(Collectors.toList());
		assertThat(tags).containsExactlyInAnyOrder(tagNames);
	}

}
