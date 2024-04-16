import com.apollographql.apollo3.ast.*


data class Field(val name: String, val type: GQLType)
class VisitorScope(
    private val schema: Schema,
    private val fragments: Map<String, GQLFragmentDefinition>,
    private val typeToVisit: String
) {
    private val path = mutableListOf<List<Field>>()

    fun paths(operation: GQLOperationDefinition): List<List<Field>> {
        path.clear()

        operation.selections.forEach {
            it.walk(emptyList(), schema.rootTypeNameFor(operation.operationType))
        }
        return path.toList()
    }

    private fun GQLSelection.walk(path: List<Field>, parentType: String) {
        when (this) {
            is GQLField -> {
                val fieldDefinition = definitionFromScope(schema, parentType)!!
                val newPath = path + Field(responseName(), fieldDefinition.type)
                val newParentType = fieldDefinition.type.rawType().name
                if (newParentType == typeToVisit) {
                    this@VisitorScope.path.add(newPath)
                }
                selections.forEach {
                    it.walk(newPath, newParentType)
                }
            }
            is GQLFragmentSpread -> {
                val fragment = fragments.get(name)!!
                fragment.selections.forEach {
                    it.walk(path, fragment.typeCondition.name)
                }
            }
            is GQLInlineFragment -> {
                selections.forEach {
                    it.walk(path, typeCondition?.name ?: parentType)
                }
            }
        }
    }

}