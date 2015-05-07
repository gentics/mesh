package com.gentics.mesh.nav.model;

import java.util.ArrayList;
import java.util.List;

public class NavigationElement {

	private NavigationElementType type;

	private List<NavigationElement> children = new ArrayList<NavigationElement>();

	private String name;

	private String path;

	private String target = new String("#");

	public NavigationElement() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public NavigationElementType getType() {
		return type;
	}

	public void setType(NavigationElementType type) {
		this.type = type;
	}

	public List<NavigationElement> getChildren() {
		return children;
	}
}
