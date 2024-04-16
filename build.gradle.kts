plugins {
    id("org.jetbrains.kotlin.jvm").version("1.9.22")
    id("com.apollographql.apollo3").version("4.0.0-beta.5")
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("build-logic:build-logic")
    }
}
val schema = "src/main/graphql/schema.graphqls"
val operations = fileTree("src/main/graphql/").apply {
    include("**/*.graphql")
}

val wiring = setupGraphQLPreprocessor(file(schema), operations)

apollo {
    service("service") {
        packageName.set("com.example")
        codegenModels.set("responseBased")
        schemaFiles.from("src/main/graphql/schema.graphqls")
        srcDir(wiring.operations)
    }
}

kotlin.sourceSets.getByName("main").kotlin.srcDir(wiring.kotlin)
dependencies {
    implementation("com.apollographql.apollo3:apollo-runtime")
    testImplementation(kotlin("test"))
}
