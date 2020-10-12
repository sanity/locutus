package locutus

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.kotest.core.spec.SpecExecutionOrder.Annotated

object ProjectConfig : AbstractProjectConfig() {
    override val specExecutionOrder: SpecExecutionOrder?
        get() = Annotated
}