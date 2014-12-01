package com.gentics.vertx.cailun.core;


public class CaiLunLinkResolverFactoryImpl<T extends CaiLunLinkResolver> implements LinkResolverFactory<AbstractLinkResolver> {

	@Override
	public AbstractLinkResolver createLinkResolver(String link) {
		return new CaiLunLinkResolver(link);
	}

}
