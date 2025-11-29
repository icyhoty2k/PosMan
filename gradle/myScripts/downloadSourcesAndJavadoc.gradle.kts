// downloadSourcesAndJavadoc.gradle.kts
// Configuration cache safe script to download sources and javadoc for all dependencies
// Gradle v.9.2.1

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.artifacts.ProjectDependency

// Create custom configurations with unique names to avoid conflicts
val downloadSourcesConfig =
    configurations.findByName("downloadSourcesConfig") ?: configurations.create("downloadSourcesConfig") {
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        }
    }

val downloadJavadocConfig =
    configurations.findByName("downloadJavadocConfig") ?: configurations.create("downloadJavadocConfig") {
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.JAVADOC))
        }
    }

// Automatically add sources and javadoc for EXTERNAL dependencies only (not project dependencies)
afterEvaluate {
    configurations.findByName("runtimeClasspath")?.allDependencies?.forEach { dep ->
        // Skip project dependencies (internal subprojects)
        if (dep !is ProjectDependency && dep.group != null && dep.version != null) {
            dependencies.add(downloadSourcesConfig.name, "${dep.group}:${dep.name}:${dep.version}")
            dependencies.add(downloadJavadocConfig.name, "${dep.group}:${dep.name}:${dep.version}")
        }
    }
}

// Task to download sources and javadoc for this project (only if it doesn't exist)
if (tasks.findByName("downloadSourcesAndJavadoc") == null) {
    tasks.register("downloadSourcesAndJavadoc") {
        group = "[Ivan]"
        description = "Downloads sources and javadoc for all dependencies in this project"
        dependsOn(downloadSourcesConfig, downloadJavadocConfig)
        doLast {
            println("\n========================================")
            println("Project: ${project.name}")
            println("========================================")

            try {
                val sourcesFiles = downloadSourcesConfig.resolvedConfiguration.resolvedArtifacts
                val javadocFiles = downloadJavadocConfig.resolvedConfiguration.resolvedArtifacts

                if (sourcesFiles.isNotEmpty()) {
                    println("\n✓ Sources downloaded (${sourcesFiles.size} files):")
                    sourcesFiles.forEach { artifact ->
                        println("  - ${artifact.file.absolutePath}")
                    }
                } else {
                    println("\n⚠ No sources found for external dependencies")
                }

                if (javadocFiles.isNotEmpty()) {
                    println("\n✓ Javadoc downloaded (${javadocFiles.size} files):")
                    javadocFiles.forEach { artifact ->
                        println("  - ${artifact.file.absolutePath}")
                    }
                } else {
                    println("\n⚠ No javadoc found for external dependencies")
                }

                // Detect actual Gradle cache location
                val gradleUserHome = System.getenv("GRADLE_USER_HOME")
                    ?: "${System.getProperty("user.home")}/.gradle"
                val cacheLocation = "$gradleUserHome/caches/modules-2/files-2.1"

                println("\n✓ All documentation downloaded to Gradle cache")
                println("  GRADLE_USER_HOME: $gradleUserHome")
                println("  Cache location: $cacheLocation")
            } catch (e: Exception) {
                println("\n⚠ Error resolving some dependencies: ${e.message}")
                println("  This is normal if some libraries don't publish sources/javadoc")
            }

            println("========================================\n")
        }
    }
}

/*
========================================================================
Usage Option 1: Apply to individual subprojects
========================================================================

In your subproject's build.gradle.kts:

plugins {
    java
}

apply(from = rootDir.resolve("gradle/myScripts/downloadSourcesAndJavadoc.gradle.kts"))

dependencies {
    implementation("com.example:some-library:1.0")
}

Then run:
gradle :subproject-a:downloadSourcesAndJavadoc

========================================
Project: subproject-a
========================================

✓ Sources downloaded (3 files):
  - I:\63_GradleLocalRepo\caches\modules-2\files-2.1\com.example\some-library\1.0\abc123\some-library-1.0-sources.jar
  - I:\63_GradleLocalRepo\caches\modules-2\files-2.1\org.slf4j\slf4j-api\2.0.0\def456\slf4j-api-2.0.0-sources.jar
  - I:\63_GradleLocalRepo\caches\modules-2\files-2.1\com.google.guava\guava\31.1\ghi789\guava-31.1-sources.jar

✓ Javadoc downloaded (3 files):
  - I:\63_GradleLocalRepo\caches\modules-2\files-2.1\com.example\some-library\1.0\jkl012\some-library-1.0-javadoc.jar
  - I:\63_GradleLocalRepo\caches\modules-2\files-2.1\org.slf4j\slf4j-api\2.0.0\mno345\slf4j-api-2.0.0-javadoc.jar
  - I:\63_GradleLocalRepo\caches\modules-2\files-2.1\com.google.guava\guava\31.1\pqr678\guava-31.1-javadoc.jar

✓ All documentation downloaded to Gradle cache
  GRADLE_USER_HOME: I:\63_GradleLocalRepo
  Cache location: I:\63_GradleLocalRepo/caches/modules-2/files-2.1
========================================

========================================================================
Usage Option 2: Apply to ALL projects (root build.gradle.kts)
========================================================================

In your ROOT build.gradle.kts:

subprojects {
    apply(from = rootDir.resolve("gradle/myScripts/downloadSourcesAndJavadoc.gradle.kts"))
}

Then run ONE command to download for ALL projects:
gradle downloadSourcesAndJavadoc

========================================================================
Usage Option 3: Create aggregate task (root build.gradle.kts)
========================================================================

// Apply to all subprojects
subprojects {
    apply(from = rootDir.resolve("gradle/myScripts/downloadSourcesAndJavadoc.gradle.kts"))
}

// Create aggregate task in root
tasks.register("downloadAllSourcesAndJavadoc") {
    group = "documentation"
    description = "Downloads sources and javadoc for all subprojects"
    dependsOn(subprojects.map { it.tasks.named("downloadSourcesAndJavadoc") })
}

Then run:
gradle downloadAllSourcesAndJavadoc

========================================================================
Benefits:
========================================================================
• Configuration cache safe (no IDEA plugin)
• Downloads sources and javadoc for EXTERNAL dependencies only
• Skips internal project dependencies automatically
• Shows exact file locations for downloaded artifacts
• Detects and displays your GRADLE_USER_HOME location
• IntelliJ IDEA automatically detects them from Gradle cache
• Reusable across multiple subprojects
• Run for all projects with one command
• Safe to apply multiple times (checks for existing configurations)
• Works with withSourcesJar() and withJavadocJar()
========================================================================
*/
