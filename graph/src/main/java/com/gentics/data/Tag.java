package com.gentics.data;

import java.util.List;

public class Tag {
	protected String label;

	protected List<Tag> children;

	public Tag() {
	}

	public Tag(String label) {
		setLabel(label);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<Tag> getChildren() {
		return children;
	}

	public void setChildren(List<Tag> children) {
		this.children = children;
	}
}
