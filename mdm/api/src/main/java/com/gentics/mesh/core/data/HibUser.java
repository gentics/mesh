package com.gentics.mesh.core.data;

public interface HibUser {

	void setCreated(HibUser creator);

	String getUuid();

	void setName(String name);

}
