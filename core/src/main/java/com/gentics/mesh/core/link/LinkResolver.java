package com.gentics.mesh.core.link;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.service.NodeService;

/**
 * This class will resolve mesh link placeholders.
 * 
 * @author johannes2
 *
 */
public class LinkResolver extends AbstractLinkResolver {

	private NodeService nodeService;

	public LinkResolver() {
		super(null);
	}

	public LinkResolver(String text, NodeService nodeRepository) {
		super(text);
		this.nodeService = nodeRepository;
	}

	@Override
	public String call() throws Exception {
		String link = get();
		if (StringUtils.isEmpty(link)) {
			return "#";
		}
		int start = link.indexOf('(') + 1;
		int end = link.indexOf(')', start);
		// Extract page id
		String idString = link.substring(start, end);
		Long id = Long.valueOf(idString);
		return getPathForPageId(id);
	}

	/**
	 * Return the shortest path to the root node for this page.
	 * 
	 * @param id
	 * @return
	 */
	private String getPathForPageId(Long id) {
		// TODO exception handling
		// TODO use GenericContentUtils
		// return pageRepo.getPath(id);
		return "WIP";
	}

}
