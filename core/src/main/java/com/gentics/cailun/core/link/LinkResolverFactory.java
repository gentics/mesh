package com.gentics.cailun.core.link;



public interface LinkResolverFactory<T extends AbstractLinkResolver> {

	public T createLinkResolver(String link);

}
