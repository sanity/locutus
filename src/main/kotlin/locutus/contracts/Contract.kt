package locutus.contracts

interface Contract<S : Any, C : Contract<S, C>> {
    fun initialSummary() : S

    fun fold(summary : S, contract : C) : S
}

interface Key<S : Any, C : Contract<S, C>> {
    class ContractSummary<S : Any, C : Contract<S, C>>(val contract : Contract<S, C>, val summary : S)
    fun verify(summaries : List<ContractSummary<S, C>>) : Boolean
}