import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import java.io.File

class PreprocessorWiring(
    val operations: Provider<Directory>,
    val kotlin: Provider<Directory>
)

fun Project.setupGraphQLPreprocessor(schema: File, operations: FileCollection): PreprocessorWiring {
    val graphQLOutput = tasks.register("addGraphQLFields", AddGraphQLFields::class.java) {
        it.schema.set(schema)
        it.operations.from(operations)
        it.outputDirectory.set(layout.buildDirectory.dir("graphql-preprocessor"))
    }.flatMap {
        it.outputDirectory
    }

    val kotlinOutput = tasks.register("generateVisitors", GenerateVisitors::class.java) {
        it.schema.set(schema)
        it.operations.from(operations)
        it.outputDirectory.set(layout.buildDirectory.dir("kotlin-generator"))
    }.flatMap {
        it.outputDirectory
    }

    return PreprocessorWiring(graphQLOutput, kotlinOutput)
}