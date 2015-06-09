package com.gentics.mesh.core.data.model.generic;

import org.jglue.totorom.FramedVertex;

public class AbstractPersistable extends FramedVertex {

	public Long getId() {
		return getProperty("id");
	}

	public String getUuid() {
		return getProperty("uuid");
	}

	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
	}

}
