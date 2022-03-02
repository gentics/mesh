package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;

/**
 * Graph domain model interface for a tag.
 * 
 * Tags can currently only hold a single string value. Tags are not localizable. A tag can only be assigned to a single tag family.
 */
public interface Tag
	extends MeshCoreVertex<TagResponse>, ReferenceableElement<TagReference>, UserTrackingVertex, ProjectElement, HibTag, GraphDBBucketableElement {

}
