import groovy.xml.dom.DOMCategory.attributes
import net.silver.buildsrc.BuildMeta
import sun.jvmstat.monitor.MonitoredVmUtil.jvmArgs

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
    implementation(BuildMeta.Libs.MOQUETTE)

    testImplementation(BuildMeta.Libs.JUNIT_API)// JUnit 5 API for compiling tests
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)// JUnit 5 Engine for running tests (runtime only)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)

}

tasks.test {
    useJUnitPlatform()
}
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            // Key fix: Explicitly set the --module-path for the compiler
            // This was the fix for the compilation failure
            "--module-path", configurations.getByName("runtimeClasspath").asPath
        )
    )
}
