package com.gentics.mesh.core;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.gentics.mesh.rest.RestModelTest;

@RunWith(OrientDBMeshTestSuite.class)
@SuiteClasses({
	RestModelTest.class
})
public class CoreTestSuite {
}
