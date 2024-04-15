plugins {
    `embedded-kotlin`
}

group = "build-logic"

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0-RC1")
    implementation("com.apollographql.apollo3:apollo-ast:4.0.0-beta.5")
}