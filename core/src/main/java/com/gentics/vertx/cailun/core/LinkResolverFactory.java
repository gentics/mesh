package com.gentics.vertx.cailun.core;


public interface LinkResolverFactory<T extends AbstractLinkResolver> {

	public T createLinkResolver(String link);

}
