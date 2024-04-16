import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateVisitors : DefaultTask() {
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
        val typeToVisit = "Flow"
        val packageName = "com.example"
        val fragmentName = "FlowStep"
        val schema = schema.get().asFile.toGQLDocument().toSchema()

        val definitions = operations.files.flatMap { it.toGQLDocument().definitions }

        val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
        val operations = definitions.filterIsInstance<GQLOperationDefinition>()

        val scope = VisitorScope(schema, fragments, typeToVisit)

        val codegen = VisitorBuilder(packageName, fragmentName)
        FileSpec.builder(packageName, "GeneratedVisitors")
            .addFunction(
                FunSpec.builder("cast")
                    .addModifiers(KModifier.INLINE)
                    .addTypeVariable(TypeVariableName("T").copy(reified = true))
                    .receiver(ClassName("kotlin", "Any").copy(nullable = true))
                    .returns(TypeVariableName("T"))
                    .addCode("return this as T")
                    .build()
            )
            .addFunction(
                FunSpec.builder("maybe")
                    .addTypeVariable(TypeVariableName("T"))
                    .receiver(TypeVariableName("T").copy(nullable = true))
                    .addParameter(
                        ParameterSpec.builder(
                            "block",
                            LambdaTypeName.get(
                                parameters = listOf(ParameterSpec.unnamed(TypeVariableName("T"))),
                                returnType = ClassName("kotlin", "Unit")
                            )
                        ).build()
                    )
                    .returns(ClassName("kotlin", "Unit"))
                    .addCode("return if(this != null) block(this) else %T", ClassName("kotlin", "Unit"))
                    .build()
            )
            // public inline fun <T> T?.maybe(block: (T) -> Unit) = if (this != null) block(this) else Unit
            .addType(
                TypeSpec.interfaceBuilder("Visitor")
                    .addModifiers(KModifier.FUN)
                    .addFunction(
                        FunSpec.builder("visit")
                            .addModifiers(KModifier.ABSTRACT)
                            .addParameter("data", ClassName("kotlin", "Any").copy(nullable = true))
                            .addParameter(
                                "block", LambdaTypeName.get(
                                    parameters = listOf(
                                        ParameterSpec.unnamed(ClassName("$packageName.fragment", fragmentName))
                                    ),
                                    returnType = ClassName("kotlin", "Unit")
                                )
                            )
                            .build()
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder(
                    "generatedVisitors",
                    ClassName("kotlin.collections", "Map")
                        .parameterizedBy(
                            ClassName("kotlin", "String"),
                            ClassName(packageName, "Visitor")
                        )
                ).initializer(
                    CodeBlock.Builder().apply {
                        add("mapOf(\n")
                        indent()
                        operations.forEach {
                            add(codegen.visitorCodeblock(it.name!!, scope.paths(it)))
                            add(",\n")
                        }
                        unindent()
                        add(")")
                    }.build()
                )
                    .build()
            )
            .build()
            .writeTo(outputDirectory.get().asFile)

    }


    @OptIn(ApolloExperimental::class)
    private fun List<GQLSelection>.alwaysGreet(schema: Schema, parentType: String): List<GQLSelection> {
        val selections = this.map {
            when (it) {
                is GQLField -> it.copy(
                    selections = it.selections.alwaysGreet(
                        schema,
                        it.definitionFromScope(schema, parentType)!!.type.rawType().name
                    )
                )

                is GQLFragmentSpread -> it
                is GQLInlineFragment -> it.copy(
                    selections = it.selections.alwaysGreet(schema, it.typeCondition?.name ?: parentType)
                )
            }
        }

        return if (parentType == "Friend" && selections.none { it is GQLField && it.responseName() == "greet" }) {
            selections + GQLField(null, null, "greet", emptyList(), emptyList(), emptyList())
        } else {
            selections
        }
    }
}