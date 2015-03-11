package com.gentics.cailun.core.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class MoveVerticle extends AbstractCoreApiVerticle {

	protected MoveVerticle() {
		super("move");
	}

	@Override
	public void registerEndPoints() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
