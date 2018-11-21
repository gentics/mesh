package com.gentics.diktyo.orientdb3.domain;

import com.gentics.diktyo.orientdb3.wrapper.element.AbstractWrappedVertex;

public class PersonImpl extends AbstractWrappedVertex implements Person {

	@Override
	public void setName(String name) {
		property("name", name);
	}

	@Override
	public String getName() {
		return property("name");
	}

	@Override
	public void setJob(Job job) {
		setLinkOut(job, "HAS_JOB");
	}

	@Override
	public Job getJob() {
		return out("HAS_JOB").next(JobImpl.class);
	}

}
