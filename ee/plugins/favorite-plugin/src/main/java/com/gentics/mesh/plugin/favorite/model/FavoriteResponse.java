package com.gentics.mesh.plugin.favorite.model;

import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for the favorite response
 */
public class FavoriteResponse implements RestModel {
	
	private List<String> favorites;

	public List<String> getFavorites() {
		return favorites;
	}

	public FavoriteResponse setFavorites(List<String> favorites) {
		this.favorites = favorites;
		return this;
	}

}
