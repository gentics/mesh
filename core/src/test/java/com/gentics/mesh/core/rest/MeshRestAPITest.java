package com.gentics.mesh.core.rest;

import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.node.NodeVerticleTest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshRestAPITest extends AbstractRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeVerticleTest.class);

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void test404Response() {
		Future<UserResponse> future = getClient().findUserByUuid("blub");
		latchFor(future);

	}

}
