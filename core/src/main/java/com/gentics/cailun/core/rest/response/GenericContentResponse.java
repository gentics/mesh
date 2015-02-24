package com.gentics.cailun.core.rest.response;

import com.gentics.cailun.core.data.model.auth.User;

public class GenericContentResponse {

	private String name;
	private String filename;
	private String content;
	private String teaser;
	private RestUser author;

	public GenericContentResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public RestUser getAuthor() {
		return author;
	}

	public void setAuthor(RestUser author) {
		this.author = author;
	}

	public String getTeaser() {
		return teaser;
	}

	public void setTeaser(String teaser) {
		this.teaser = teaser;
	}

}
