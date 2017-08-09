package com.gentics.mesh.search;

public class MeshSearchHit<T> {
	public String uuid;
	public String language;
	public T element;

	public MeshSearchHit(String uuid, String language) {
		this.uuid = uuid;
		this.language = language;
	}
}
