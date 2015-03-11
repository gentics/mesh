package com.gentics.cailun.core.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class CopyVerticle extends AbstractCoreApiVerticle {

	protected CopyVerticle() {
		super("copy");
	}

	@Override
	public void registerEndPoints() throws Exception {
		// TODO Auto-generated method stub

	}

}
