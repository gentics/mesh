package com.gentics.cailun.core;


public interface LinkResolverFactory<T extends AbstractLinkResolver> {

	public T createLinkResolver(String link);

}
