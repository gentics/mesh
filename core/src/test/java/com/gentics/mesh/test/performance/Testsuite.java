package com.gentics.mesh.test.performance;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name = "testsuite")
public class Testsuite {

	@Attribute(name = "name")
	private String name;

	@Attribute(name = "time")
	private String time;

	@ElementList(inline = true)
	private List<Testcase> testcases = new ArrayList<>();

	public Testsuite(Class<?> clazz, double time) {
		this.name = clazz.getName();
		this.time = String.valueOf(time);
	}

	public List<Testcase> getTestcases() {
		return testcases;
	}

	public void setTestcases(List<Testcase> testcases) {
		this.testcases = testcases;
	}

}
