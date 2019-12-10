package com.gentics.mesh.cli.schema;

import com.gentics.mesh.cli.MeshCommand;
import com.gentics.mesh.cli.schema.sub.MigrateSchemaCommand;

import picocli.CommandLine.Command;

@Command(name = "schema", description = "Schema commands", subcommands = { MigrateSchemaCommand.class })
public class SchemaCommand extends MeshCommand {

}
