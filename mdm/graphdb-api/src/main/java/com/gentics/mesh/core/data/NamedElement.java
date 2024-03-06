package com.gentics.mesh.core.data;

import com.gentics.mesh.graphdb.model.MeshElement;

/**
 * A named element is a mesh element that can be identified by a name. Elements such as roles, users, tags, groups may implement this interface in order to
 * provide a common way to extract the name of the element. This way a generic way of retrieving a element name is created for e.g. logging purposes.
 */
public interface NamedElement extends MeshElement, HibNamedElement {

}
