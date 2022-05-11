package com.gentics.mesh.test;

public class NoMeshTestContextException extends IllegalStateException {

	private static final long serialVersionUID = -4224609585454911218L;

	public NoMeshTestContextException(Class<?> classOfT) {
		super("No implementations of " 
				+ classOfT.getCanonicalName() 
				+ " found in either com.gentics.mesh package or system properties."
				+ " Have you started the test outside of the *tests-runner / *tests-context project?");
	}
}
