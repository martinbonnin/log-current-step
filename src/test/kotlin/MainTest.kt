import com.example.GetFlowQuery
import com.example.GetNestedFlowQuery
import com.example.fragment.FlowStep
import kotlin.test.Test

class MainTest {
    @Test
    fun testStuff() {
        val data = GetFlowQuery.Data(
            flowClaimBarNext = GetFlowQuery.Data.FlowClaimBarNext(
                __typename = "foobar",
                currentStep = GetFlowQuery.Data.FlowClaimBarNext.CurrentStep(
                    id = "bar",
                    value = 42
                )
            )
        )

        com.example.generatedVisitors.get("getFlow")!!.visit(data) {
            println("id: ${it.currentStep.id}")
            println("value: ${it.currentStep.value}")
        }
    }
}

