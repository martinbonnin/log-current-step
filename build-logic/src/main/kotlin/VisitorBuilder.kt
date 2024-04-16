import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import org.gradle.configurationcache.extensions.capitalized

class VisitorBuilder(val packageName: String, val fragmentName: String) {
    fun visitorCodeblock(operationName: String, paths: List<List<Field>>): CodeBlock {
        return CodeBlock.Builder().apply {
            add("%S to Visitor { data, block ->\n", operationName)
            indent()
            add("data as %T\n", ClassName(packageName, operationName.capitalized() + "Query", "Data"))
            paths.forEach {
                add("data.")
                add(it.codeblock())
                add("\n")
            }
            unindent()
            add("}")
        }.build()
    }

    private fun List<Field>.codeblock(): CodeBlock {
        return CodeBlock.Builder().apply {
            val field = first()
            add("%L", field.name)
            add(codeblock2())
        }.build()
    }
    private fun List<Field>.codeblock2(): CodeBlock {
        return CodeBlock.Builder().apply {
            val field = first()
            val next = drop(1)
            if (field.type !is GQLNonNullType) {
                add("?")
                add((listOf(field.copy(type = GQLNonNullType(null, field.type))) + next).codeblock2())
                return@apply
            }

            val type = field.type.type
            if (type is GQLListType) {
                add(".forEach {\n")
                indent()
                add("it%L\n", (listOf(field.copy(type = type.type)) + next).codeblock2())
                unindent()
                add("}")
            } else {
                if (next.isEmpty()) {
                    add(".cast<%T>().maybe(block)", ClassName("$packageName.fragment", fragmentName))
                } else {
                    add(".")
                    add(next.codeblock())
                }
            }
        }.build()
    }
}