import net.silver.buildsrc.BuildMeta

plugins {
    id("java")
}

group = "net.silver.services"
version = project.version


tasks.jar {
    archiveBaseName.set(project.name)
    archiveVersion.set("$version")
    archiveClassifier.set("")
}


dependencies {
    testImplementation(BuildMeta.Libs.JUNIT_API)// JUnit 5 API for compiling tests
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)// JUnit 5 Engine for running tests (runtime only)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)

}

tasks.test {
    useJUnitPlatform()
}
