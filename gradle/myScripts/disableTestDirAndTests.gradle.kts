// disableTestDirAndTests.gradle.kts
// Gradle v.9.2.1
// This keeps the test source set (for IDE compatibility) but makes it completely empty

import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

val sourceSets = extensions.findByName("sourceSets") as? SourceSetContainer

if (sourceSets != null) {
    val testSourceSet = sourceSets.findByName("test")
    if (testSourceSet != null) {
        // Completely empty all test directories
        testSourceSet.java.setSrcDirs(emptyList<File>())
        testSourceSet.resources.setSrcDirs(emptyList<File>())
        testSourceSet.allJava.setSrcDirs(emptyList<File>())
        testSourceSet.allSource.setSrcDirs(emptyList<File>())
        //  println("✓ Test source set disabled in project: ${project.name}")
    }

    // Disable all test tasks
    tasks.withType(Test::class).configureEach {
        enabled = false
    }
}

/*
========================================================================
Usage in your subproject's build.gradle.kts:
========================================================================

Option 1: Using rootDir (RECOMMENDED - works for any nesting level)
------------------------------------------------------------------------
plugins {
    java
}

// Apply the shared script from root project
apply(from = rootDir.resolve("gradle/myScripts/disableTestDirAndTests.gradle.kts"))

dependencies {
    implementation("com.example:some-library:1.0")
}

------------------------------------------------------------------------

Option 2: Using relative path
------------------------------------------------------------------------
plugins {
    java
}

// Path relative to current build file (adjust ../ based on nesting)
apply(from = "../gradle/myScripts/disableTestDirAndTests.gradle.kts")

dependencies {
    implementation("com.example:some-library:1.0")
}

========================================================================
Directory Structure Example:
========================================================================
project-root/
├── gradle/
│   └── myScripts/
│       └── disableTestDirAndTests.gradle.kts  ← This file
├── subproject-a/
│   ├── build.gradle.kts                       ← Applies the script
│   └── src/
│       └── main/
│           └── java/
├── subproject-b/
│   ├── build.gradle.kts                       ← Applies the script
│   └── src/
│       └── main/
│           └── java/
└── settings.gradle.kts

========================================================================
Benefits of this approach:
========================================================================
• Removes test source set completely (no src/test/java expected)
• Disables all test tasks
• Reusable across multiple subprojects
• Single source of truth for test removal logic
• IntelliJ IDEA won't mark test directories
• Cleaner project structure
========================================================================
can i apply it anywhere in build script not just on top
Yes! Since the script is now just emptying the test source set (not removing it), you can apply it anywhere in your build script. The order doesn't matter as much because:

It's not removing anything (so nothing will look for a missing source set)
It's just configuring the existing test source set to have empty directories
Test task disabling works regardless of when it's applied
// Gradle v.9.2.1
*/
/*
=========================================================
just copy  this anywhere in build.gradle.kts in subproject where thest are not required

//disable tests and hide test dirs src-test nd resources-test
apply(from = rootDir.resolve("gradle/myScripts/disableTestDirAndTests.gradle.kts"))


 */
