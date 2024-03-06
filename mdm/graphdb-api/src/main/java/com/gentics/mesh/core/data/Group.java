package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;

/**
 * Graph domain model interface for groups.
 */
public interface Group extends MeshCoreVertex<GroupResponse>, ReferenceableElement<GroupReference>, UserTrackingVertex, HibGroup, GraphDBBucketableElement {
}
