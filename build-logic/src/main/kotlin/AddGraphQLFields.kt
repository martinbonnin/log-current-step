import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.*
import okio.buffer
import okio.sink
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class AddGraphQLFields: DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val schema: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val operations: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @OptIn(ApolloExperimental::class)
    @TaskAction
    fun taskAction() {
        val schema = schema.get().asFile.toGQLDocument().toSchema()

        operations.files.flatMap { it.toGQLDocument().definitions }
            .map {
                when (it) {
                    is GQLOperationDefinition -> it.copy(
                        selections = it.selections.addFlowStep(schema, schema.rootTypeNameFor(it.operationType))
                    )
                    is GQLFragmentDefinition -> if (it.name != "flowStep") {
                        it.copy(
                            selections = it.selections.addFlowStep(schema, it.typeCondition.name)
                        )
                    } else {
                        it
                    }
                    else -> it
                }
            }
            .let {
                GQLDocument(definitions = it, sourceLocation = null)
            }.let { doc ->
                outputDirectory.get().asFile.resolve("operations.graphql").outputStream().sink().buffer().use { bs ->
                    doc.toUtf8(bs, "  ")
                }
            }
    }


    @OptIn(ApolloExperimental::class)
    private fun List<GQLSelection>.addFlowStep(schema: Schema, parentType: String): List<GQLSelection> {
        val selections = this.map {
            when (it) {
                is GQLField -> it.copy(
                    selections = it.selections.addFlowStep(schema, it.definitionFromScope(schema, parentType)!!.type.rawType().name)
                )
                is GQLFragmentSpread -> it
                is GQLInlineFragment -> it.copy(
                    selections = it.selections.addFlowStep(schema, it.typeCondition?.name ?: parentType)
                )
            }
        }

        return if (parentType == "Flow" && selections.none { it is GQLFragmentSpread && it.name == "flowStep" }) {
            selections + GQLFragmentSpread(null, "flowStep", emptyList(), )
        } else {
            selections
        }
    }
}