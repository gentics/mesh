package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshVertex;

public interface SearchQueueRoot extends MeshVertex {

	void removeElement(GenericVertex<?> element);

	void addElement(GenericVertex<?> element);

	GenericVertex<?> getNext();

}
