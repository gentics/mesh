package com.gentics.mesh.core.link;

public interface LinkResolverFactory<T extends AbstractLinkResolver> {

	public T createLinkResolver(String link);

}
