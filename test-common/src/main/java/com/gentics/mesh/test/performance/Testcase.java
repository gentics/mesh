package com.gentics.mesh.test.performance;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "testcase")
public class Testcase {

	@Attribute(name = "name")
	private String name;

	@Attribute(name = "className")
	private String className;
	
	@Attribute(name = "time")
	private String time;

	public Testcase(String name, String className, String time) {
		this.name = name;
		this.className = className;
		this.time = time;
	}
}
