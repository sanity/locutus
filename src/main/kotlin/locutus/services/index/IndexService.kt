package locutus.services.index

class IndexService() {
}

interface Aggregator<V : Any, A : Any> {
    fun aggregate(vararg aggregateValue : ValueOrAggregate<V, A>) : A
}

sealed class ValueOrAggregate<V : Any, A : Any> {
    abstract fun distance(other : ValueOrAggregate<V, A>) : Double
    abstract class Value<V : Any, A : Any>(val value : V) : ValueOrAggregate<V, A>()
    abstract class Aggregate<V : Any, A : Any>(val aggregate : A) : ValueOrAggregate<V, A>()

}