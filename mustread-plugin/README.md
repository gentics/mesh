# Must Read Plugin

This plugin provides endpoints to fetch all nodes that must be read
and to set specific nodes to be already read.

# Setup

Copy the jar file and the `config` directory into the plugin directory 
of your mesh Installation. Modify the config.yml and graphql/* files
for your needs.

# Configuration

## Config.yml

The config file allows you to customize the behaviour of the must read 
plugin. The projects array allows you to define several project for 
which the must read plugin is active.

### NodeListParameterName

With this parameter you define the name for the field containing the
uuids that are read for the unread endpoint. If you leave it blank the
default value `uuids` is used

### Projects

You can define the following parameters for each project:

* Name: Name of the Mesh Project the plugin is active
* FolderName: Name of the folder where the plugin stores it's informations
* MustReadGraphQLFile: GraphQL file that fetches the ids of the nodes 
that mus be read
* UUIDFieldName: Name of the field in the MustReadGrapQLFile that stores
the uuid. If you don't specify it or specify it as empty the plugin assumes
you meant `uuid` 
* DataGraphQLFile: GraphQL file that fetches the data you want to receive
when accessing the /unread endpoint. This GraphQL file gets the 
variable $uuids provides by the module to fetch the data for the unread
nodes

**The must read plugin only activates for projects which are defined in
the config.yml and have `FolderName`, `MustReadGraphQLFile` and 
`DataGraphQLFile` set**