package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.core.rest.node.NodeResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * POJO for information needed for requests to a binary check service.
 */
public class BinaryCheckContext {

	/** The node response for the current save request. */
	private NodeResponse node;
	/** The check service URL. */
	private String checkServiceUrl;
	/** The check secret. */
	private String checkSecret;
	/** The uploaded binaries filename. */
	private String filename;
	/** The uploaded binaries content type. */
	private String contentType;

	public boolean needsCheck() {
		return StringUtils.isNotBlank(checkServiceUrl);
	}

	public NodeResponse getNode() {
		return node;
	}

	public BinaryCheckContext setNode(NodeResponse node) {
		this.node = node;
		return this;
	}

	public String getCheckServiceUrl() {
		return checkServiceUrl;
	}

	public BinaryCheckContext setCheckServiceUrl(String checkServiceUrl) {
		this.checkServiceUrl = checkServiceUrl;
		return this;
	}

	public String getCheckSecret() {
		return checkSecret;
	}

	public BinaryCheckContext setCheckSecret(String checkSecret) {
		this.checkSecret = checkSecret;
		return this;
	}

	public String getFilename() {
		return filename;
	}

	public BinaryCheckContext setFilename(String filename) {
		this.filename = filename;
		return this;
	}

	public String getContentType() {
		return contentType;
	}

	public BinaryCheckContext setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}
}
