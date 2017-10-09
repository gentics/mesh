
[options="header"]
|======
| Configuration             | Type  | Default | Description
| ```cluster.enabled```     | Path  | false   |  This setting is optional and can be used to configure the network which the cluster daemons bind to. 
Gentics Mesh will try to determine the network automatically if no setting has been provided. 
The value of this setting will currently also be used to connect other instances to the configured instance. 
So make sure that the IP/Host can be reached from other potential instances in your network.
| ```cluster.networkHost``` | String | - | Host which will be used to bind clustering related ports
| ```cluster.clusterName``` | String | - | Name of the cluster which will be used to group multiple instances together.
The setting is required when clustering is enabled. 
It can also be used to form a new cluster next to an existing cluster. 
Only instances with the same clusterName will be able to find eachother.
| ```cluster.vertxPort```   | Number | 0 (random) | The vertxPort setting is used by the Vert.x eventbus message service.
By default vert.x will choose any free port and utilize it for the service.
| ```nodeName```            | String | -  | The node name is used to identify the instance in the cluster. 
The name must be unique to a single instance and should not be changed.
|======
