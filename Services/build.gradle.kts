import jdk.jfr.internal.JVM.exclude
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
    implementation(platform("io.netty:netty-bom:4.1.128.Final"))
    implementation("de.fraunhofer.iosb.io.moquette:moquette-broker:0.15.1") {

        // <--- ADD THIS LINE --->
        exclude(group = "org.slf4j", module = "slf4j-reload4j")
    }


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
