package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;

public class MiscExamples extends AbstractExamples {

	public static LoginRequest getLoginRequest() {
		LoginRequest request = new LoginRequest();
		request.setUsername("admin");
		request.setPassword("finger");
		return request;
	}

	public SearchStatusResponse searchStatusJson() {
		SearchStatusResponse status = new SearchStatusResponse();
		status.setBatchCount(42);
		return status;
	}

	public GenericMessageResponse genericResponse() {
		//TODO allow for custom messages
		GenericMessageResponse message = new GenericMessageResponse();
		message.setMessage("I18n message");
		return message;
	}

}
