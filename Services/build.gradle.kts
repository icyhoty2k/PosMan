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
    implementation(platform("io.netty:netty-bom:4.2.7.Final"))

    // 2. Select only the necessary Netty modules (no version needed thanks to the BOM)
    implementation("io.netty:netty-transport") // Core Nio transport layer
    implementation("io.netty:netty-handler")      // Core handler chain logic
    implementation("io.netty:netty-codec-mqtt")   // MQTT protocol encoding/decoding
    implementation("io.netty:netty-buffer")       // Byte buffer management

    testImplementation(BuildMeta.Libs.JUNIT_API)// JUnit 5 API for compiling tests
    testImplementation(BuildMeta.Libs.JUNIT_JUPITER)// JUnit 5 Engine for running tests (runtime only)
    testRuntimeOnly(BuildMeta.Libs.JUNIT_PLATFORM)

}
tasks.test {
    useJUnitPlatform()
}
//tasks.withType<JavaCompile> {
//    options.compilerArgs.addAll(
//        listOf(
//            "--add-reads", "net.silver.services=ALL-UNNAMED"
//        )
//    )
//}
//tasks.withType<JavaCompile>().configureEach {
//    options.compilerArgs.add(
//        // Use -Xlint:none to suppress all warnings, including deprecation,
//        // which gives you a clean build.
//        // options.compilerArgs.addAll(listOf("-Xlint:none"))
//
//        // Alternatively, use -Xlint:deprecation to get details, but this
//        // will still cause the warning message to appear in the output.
//        // For a clean terminal output, use the code below:
//        "-Xlint:deprecation"
//    )
//}
