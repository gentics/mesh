package com.gentics.cailun.core.rest.file.response;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class RestBinaryFile extends AbstractRestModel {

	private String filename;

	private String[] perms;

	public RestBinaryFile() {
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
