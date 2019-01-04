
* com.syncleus.ferma.tx.Tx -> com.gentics.madl.tx.Tx

`find -name "*.java" -exec sed -i 's/com\.syncleus\.ferma\.tx\.Tx/com.gentics.madl.tx.Tx/g' {} \;`

* com.syncleus.ferma.AbstractEdgeFrame -> com.gentics.madl.orientdb3.wrapper.element.AbstractWrappedEdge

`find -name "*.java" -exec sed -i 's/com\.syncleus\.ferma\.AbstractEdgeFrame/com.gentics.madl.orientdb3.wrapper.element.AbstractWrappedEdge/g' {} \;`

* com.syncleus.ferma.AbstractVertexFrame -> com.gentics.madl.orientdb3.wrapper.element.AbstractWrappedVertex

`find -name "*.java" -exec sed -i 's/com\.syncleus\.ferma\.AbstractVertexFrame/com.gentics.madl.orientdb3.wrapper.element.AbstractWrappedVertex/g' {} \;`

* com.syncleus.ferma.EdgeFrame -> com.gentics.madl.wrapper.element.WrappedEdge

`find -name "*.java" -exec sed -i 's/com\.syncleus\.ferma\.EdgeFrame/com.gentics.madl.wrapper.element.WrappedEdge/g' {} \;`

* com.syncleus.ferma.VertexFrame -> com.gentics.madl.wrapper.element.WrappedVertex

`find -name "*.java" -exec sed -i 's/com\.syncleus\.ferma\.VertexFrame/com.gentics.madl.wrapper.element.WrappedVertex/g' {} \;`

* com.syncleus.ferma.annotations.GraphElement -> com.gentics.madl.annotation.GraphElement

`find -name "*.java" -exec sed -i 's/com\.syncleus\.ferma\.annotations\.GraphElement/com.gentics.madl.annotation.GraphElement/g' {} \;`

* com.tinkerpop.blueprints.Vertex -> org.apache.tinkerpop.gremlin.structure.Vertex

`find -name "*.java" -exec sed -i 's/com\.tinkerpop\.blueprints\.Vertex/org.apache.tinkerpop.gremlin.structure.Vertex/g' {} \;`

* com.tinkerpop.blueprints.Edge -> org.apache.tinkerpop.gremlin.structure.Edge

`find -name "*.java" -exec sed -i 's/com\.tinkerpop\.blueprints\.Edge/org.apache.tinkerpop.gremlin.structure.Edge/g' {} \;`

* com.tinkerpop.blueprints.Direction - > org.apache.tinkerpop.gremlin.structure.Direction
```
find -name "*.java" -exec sed -i 's/com\.tinkerpop\.blueprints\.Direction/org.apache.tinkerpop.gremlin.structure.Direction/g' {} \;
```




* getGraph().addFramedVertex -> createVertex
```
find -name "*.java" -exec sed -i 's/getGraph()\.addFramedVertex/createVertex/g' {} \;
find -name "*.java" -exec sed -i 's/VertexFrame/WrappedVertex/g' {} \;
find -name "*.java" -exec sed -i 's/EdgeFrame/WrappedEdge/g' {} \;
find -name "*.java" -exec sed -i 's/ElementFrame/WrappedEdge/g' {} \;
```

VertexFrame -> WrappedVertex

TransactionalGraph -> 

* FramedGraph -> Database
```
find -name "*.java" -exec sed -i 's/FramedGraph/Database/g' {} \;
```
com.syncleus.ferma.typeresolvers.TypeResolver -> 

