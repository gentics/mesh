This folder contains a set of test plugins which will be build to be used for our plugin tests.

You can use these plugin in your IDE for testing and development purposes. A custom profile can be used to set the needed `mesh.version` property.

```
…
<profile>
    <id>mesh-version</id>
    <activation>
        <activeByDefault>false</activeByDefault>
    </activation>
    <properties>
        <mesh.version>0.36.4-SNAPSHOT</mesh.version>
    </properties>
</profile>
…
```

