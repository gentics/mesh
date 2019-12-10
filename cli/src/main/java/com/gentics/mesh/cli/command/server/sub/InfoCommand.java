package com.gentics.mesh.cli.command.server.sub;

import com.gentics.mesh.cli.MeshCommand;
import com.gentics.mesh.rest.client.MeshRestClient;

import picocli.CommandLine.Command;

@Command(name = "info", mixinStandardHelpOptions = true, description = "Info command")
public class InfoCommand extends MeshCommand {

	@Override
	public Integer call() throws Exception {
		MeshRestClient client = MeshRestClient.create("demo.getmesh.io", 443, true);
		System.out.println(client.meshStatus().blockingGet().toJson());
		return 0;
	}

}
