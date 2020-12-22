package com.gentics.mesh.search;

/**
 * POJO for a search hit.
 * 
 * @param <T> Element type
 */
public class MeshSearchHit<T> {

	public String uuid;
	public String language;
	public T element;

	public MeshSearchHit(String uuid, String language) {
		this.uuid = uuid;
		this.language = language;
	}
}
