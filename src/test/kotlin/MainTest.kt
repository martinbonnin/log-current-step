import com.example.AlwaysGreetQuery
import com.example.fragment.QueryFragment
import kotlin.test.Test

class MainTest {
    @Test
    fun testStuff() {
        AlwaysGreetQuery.Data(
            "Query",
            AlwaysGreetQuery.Node(
                "User",
                onUser = AlwaysGreetQuery.OnUser(
                    friend = listOf(
                        AlwaysGreetQuery.Friend(
                            "Friend",
                            "Hello \uD83D\uDC4B"
                        )
                    )
                )
            ),
            friend = AlwaysGreetQuery.Friend1(
                "Friend",
                greet = "Bonjour",
            ),
            queryFragment = QueryFragment(
                user = QueryFragment.User(
                    friend = listOf(
                        QueryFragment.Friend(
                            "Friend",
                            greet = "Gutten Morgen"
                        )
                    )
                )
            )
        )
    }
}