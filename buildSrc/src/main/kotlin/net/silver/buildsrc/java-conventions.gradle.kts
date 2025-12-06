import gradle.kotlin.dsl.accessors._d7c1cb8291fcf7e869bfba85a0dc6ae2.java
import net.silver.buildsrc.BuildMeta
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    java
}

// Apply shared configuration for all Java projects
group = BuildMeta.MAIN_GROUP
version = BuildMeta.VERSION_PARTIAL_NO_BUILD_NUMBER
// Set build directory using BuildMeta path
layout.buildDirectory.set(rootProject.file(BuildMeta.Paths.OUTPUT_BUILD_DIR + project.name))

java {
    modularity.inferModulePath.set(true)
    toolchain {
        languageVersion = JavaLanguageVersion.of(BuildMeta.JAVA_VERSION)
        vendor.set(JvmVendorSpec.MICROSOFT)
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    }
}

// Configure all JavaCompile tasks
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(BuildMeta.JAVA_VERSION)
    options.isIncremental = true
    options.isFork = true
    modularity.inferModulePath.set(true)
}

// Configure all Javadoc tasks
tasks.withType<Javadoc>().configureEach {
    val opts = options as StandardJavadocDocletOptions
    opts.addStringOption("Xdoclint:none", "-quiet")
    opts.encoding = "UTF-8"
    opts.memberLevel = JavadocMemberLevel.PUBLIC

    // Set destination directory
    destinationDir = layout.buildDirectory
        .dir("docs/javadoc")
        .get()
        .asFile
}

// Apply the downloadSourcesAndJavadoc script if it exists
val scriptFile = rootProject.file("gradle/myScripts/downloadSourcesAndJavadoc.gradle.kts")
if (scriptFile.exists()) {
    apply(from = scriptFile)
}

// Configure test tasks
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    modularity.inferModulePath.set(true)
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
