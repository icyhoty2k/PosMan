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
    implementation("org.apache.activemq:activemq-broker:6.2.0")
    implementation("org.apache.activemq:activemq-client:6.2.0")
    implementation("org.apache.activemq:activemq-mqtt:6.2.0")
    testImplementation(BuildMeta.Libs.JUNIT_API)// JUnit 5 API for compiling tests
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)// JUnit 5 Engine for running tests (runtime only)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)

}
tasks.test {
    useJUnitPlatform()
}
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "--add-reads", "net.silver.services=ALL-UNNAMED"
        )
    )
}
