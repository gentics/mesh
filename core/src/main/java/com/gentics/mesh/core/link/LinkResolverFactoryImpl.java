package com.gentics.mesh.core.link;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.gentics.mesh.core.repository.MeshNodeRepository;

/**
 * Factory which provides link resolvers
 * 
 * @author johannes2
 *
 * @param <T>
 */
@Service
@Scope("singleton")
public class LinkResolverFactoryImpl<T extends LinkResolver> implements LinkResolverFactory<AbstractLinkResolver> {

	@Autowired
	//@Qualifier("genericContentRepository")
	MeshNodeRepository contentRepository;

	private LinkResolverFactoryImpl() {
	}
	
	@Override
	public AbstractLinkResolver createLinkResolver(String link) {
		// TODO replace class with prototype spring DI
		return new LinkResolver(link, contentRepository);
	}

}
