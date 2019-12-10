package com.gentics.mesh.cli.command.job;

import com.gentics.mesh.cli.MeshCommand;
import com.gentics.mesh.cli.command.job.sub.ListCommand;
import com.gentics.mesh.cli.command.job.sub.ResetCommand;
import com.gentics.mesh.cli.command.job.sub.TriggerCommand;

import picocli.CommandLine.Command;

@Command(name = "job", aliases = { "j" }, description = "Job commands", subcommands = {
	TriggerCommand.class,
	ListCommand.class,
	ResetCommand.class })
public class JobCommand extends MeshCommand {

}
