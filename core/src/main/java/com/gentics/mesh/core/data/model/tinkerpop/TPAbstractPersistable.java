package com.gentics.mesh.core.data.model.tinkerpop;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.VertexFrame;
import com.tinkerpop.frames.modules.javahandler.JavaHandler;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("type")
public interface TPAbstractPersistable extends VertexFrame {

	@Property("id")
	@JavaHandler
	public Long getId();

	@Property("uuid")
	public String getUuid();

	@Property("uuid")
	public void setUuid(String uuid);

}
