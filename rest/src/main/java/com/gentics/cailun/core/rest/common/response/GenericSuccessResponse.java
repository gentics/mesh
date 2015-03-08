package com.gentics.cailun.core.rest.common.response;

public class GenericSuccessResponse {

	private String msg;

	public GenericSuccessResponse(String msg) {
		this.msg = msg;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
