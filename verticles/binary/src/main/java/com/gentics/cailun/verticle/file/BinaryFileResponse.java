package com.gentics.cailun.verticle.file;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class BinaryFileResponse extends AbstractRestModel {

	private String filename;

	private String[] perms = {};

	public BinaryFileResponse() {
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String[] getPerms() {
		return perms;
	}

	public void setPerms(String... perms) {
		this.perms = perms;
	}

}
