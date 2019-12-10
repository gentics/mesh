package com.gentics.mesh.cli.command.job.sub;

import com.gentics.mesh.cli.MeshCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "reset", description = "Reset the job")
public class ResetCommand extends MeshCommand {

	@Parameters(index = "0", description = "Uuid of the job that should be reset.")
	private String uuid;

}
