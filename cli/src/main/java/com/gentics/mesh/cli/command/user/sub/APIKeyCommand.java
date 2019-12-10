package com.gentics.mesh.cli.command.user.sub;

import com.gentics.mesh.cli.MeshCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "apikey", mixinStandardHelpOptions = true, description = "Generate a new API key")
public class APIKeyCommand extends MeshCommand {

	@Parameters(index = "0", description = "Uuid or name of the user")
	private String id;

}
