package com.gentics.mesh.assertj.impl;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.tag.TagReference;
import static org.junit.Assert.*;

public class NodeResponseAssert extends AbstractAssert<NodeResponseAssert, NodeResponse> {

	public NodeResponseAssert(NodeResponse actual) {
		super(actual, NodeResponseAssert.class);
	}

	/**
	 * Checks whether the given tag is listed within the node rest response.
	 * 
	 * @param restNode
	 * @param tag
	 * @return
	 */
	public boolean contains(Tag tag) {
		assertNotNull(tag);
		assertNotNull(tag.getUuid());
		assertNotNull(actual);
		assertNotEquals("There were not tags listed in the restNode.", 0, actual.getTags().size());
		if (actual.getTags() == null) {
			return false;
		}

		for (TagFamilyTagGroup group : actual.getTags().values()) {
			for (TagReference restTag : group.getItems()) {
				if (tag.getUuid().equals(restTag.getUuid())) {
					return true;
				}
			}
		}
		return false;
	}

	
}
