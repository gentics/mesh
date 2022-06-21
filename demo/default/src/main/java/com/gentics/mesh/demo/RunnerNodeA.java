package com.gentics.mesh.demo;

/**
 * Cluster runner
 */
public class RunnerNodeA extends AbstractRunnerNode {

	static {
		System.setProperty("mesh.confDirName", "config-nodeA");
	}

	public RunnerNodeA(String[] args) {
		super(args);
	}

	/**
	 * Run the server.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		new RunnerNodeA(args).run();
	}

	@Override
	protected String getBasePath() {
		return "data-nodeA";
	}

	@Override
	protected int getPort() {
		return 8080;
	}
}
