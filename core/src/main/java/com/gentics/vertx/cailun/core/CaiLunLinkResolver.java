package com.gentics.vertx.cailun.core;


public class CaiLunLinkResolver extends AbstractLinkResolver {

	public CaiLunLinkResolver(String text) {
		super(text);
	}

	@Override
	public String call() throws Exception {
		return "huhu(" + get()+ ")";
	}

}
