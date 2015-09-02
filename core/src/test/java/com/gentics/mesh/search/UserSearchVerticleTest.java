package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;

public class UserSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(userVerticle);
		return list;
	}
}
