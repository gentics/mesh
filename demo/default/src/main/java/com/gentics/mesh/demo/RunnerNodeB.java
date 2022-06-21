package com.gentics.mesh.demo;

/**
 * Cluster runner
 */
public class RunnerNodeB extends AbstractRunnerNode {

	static {
		System.setProperty("mesh.confDirName", "config-nodeB");
	}

	public RunnerNodeB(String[] args) {
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
		return "data-nodeB";
	}

	@Override
	protected int getPort() {
		return 8081;
	}
}
