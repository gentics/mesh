package com.gentics.mesh.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "sub2", mixinStandardHelpOptions = true, description = "Executes sub command 2")
public class SubCmd2 implements Callable<Integer> {

	@Override
	public Integer call() throws Exception {
		System.out.println("Called sub command 2");
		return 0;
	}

}
