package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestUserList {
	private List<RestUser> list = new ArrayList<>();

	public RestUserList() {
	}

	public List<RestUser> getList() {
		return list;
	}

	public void setList(List<RestUser> list) {
		this.list = list;
	}
}
