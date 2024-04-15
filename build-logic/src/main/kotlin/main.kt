import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import java.io.File

class PreprocessorWiring(
    val operations: Provider<Directory>
)

fun Project.setupGraphQLPreprocessor(schema: File, operations: FileCollection): PreprocessorWiring {
    return tasks.register("addGraphQLFields", AddGraphQLFields::class.java) {
        it.schema.set(schema)
        it.operations.from(operations)
        it.outputDirectory.set(layout.buildDirectory.dir("graphql-preprocessor"))
    }.flatMap {
        it.outputDirectory
    }.let {
        PreprocessorWiring(it)
    }
}