package com.gentics.mesh.plugin.favorite;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.mesh.plugin.favorite.model.FavoriteResponse;
import com.gentics.mesh.util.UUIDUtil;

public class ModelTest {

	@Test
	public void testModel() {
		FavoriteResponse resp = new FavoriteResponse();
		resp.setFavorites(Arrays.asList(UUIDUtil.randomUUID(), UUIDUtil.randomUUID()));
		System.out.println(resp.toJson());
	}
}
