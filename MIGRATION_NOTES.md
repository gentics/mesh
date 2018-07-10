## API Changes

* Packages change: `com.tinkerpop.blueprints.Direction` to `org.apache.tinkerpop.gremlin.structure.Direction`
* `edge.getVertex(IN)` -> `edge.inVertex()`
* `edge.getLabel()` -> `edge.label()`
* `vertex.getEdges(..)` -> `vertex.edges(..)`
* `element.getId()` -> `element.id()`
* `element.getProperty("key")` -> `element.value("key")`
* `element.setProperty("key", "value")` -> `element.property("key", "value")`
* `element.removeProperty("key")` -> ? 

## Accessing properties



## Iterator changes


```
for (Edge edge : v.getEdges(OUT, "HAS_ITEM)) {
…
}
```

to 

```
for (Edge edge : (Iterable<Edge>)() -> v.eEdges(OUT, "HAS_ITEM)) {
…
}
```

## GraphDB

* `TransactionalGraph` -> ?

## Problems

* mark() deprecated
* retain() deprecated
* FramedTransactionalGraph removed
* WrappedFramedGraph commit? How?