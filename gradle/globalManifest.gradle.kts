import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.api.tasks.bundling.Jar

subprojects {
    val author = "Ivan Hristov Yanev"

    // Ensure the task is a Jar task before configuring the manifest
    tasks.withType<Jar>().configureEach {
        // Calculate the timestamp when the Jar task is configured
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )

        manifest {
            attributes(
                // ===== Project Info =====
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to author,
                "Implementation-URL" to "https://github.com/icyhoty2k",

                // ===== Build Info (Highly Detailed) =====
                "Built-Timestamp" to timestamp,
                "Built-By" to "computerUserName -> " + System.getProperty("user.name"),

                // Detailed OS Information: Name/Architecture v.Version
                "Build-OS" to System.getProperty("os.name") + "/" + System.getProperty("os.arch") + " v." + System.getProperty(
                    "os.version"
                ),

                // Detailed Java Information: Vendor Version/VM_Version
                "Build-Java-Version" to System.getProperty("java.vendor") + " " + System.getProperty("java.version") + "/" + System.getProperty(
                    "java.vm.version"
                ),
                "Build-Gradle-Version" to gradle.gradleVersion,

                // ===== Legal & Custom =====
                "Author" to author,
                "License" to "MIT",
                // Calculate copyright year dynamically
                "Copyright" to "© ${LocalDateTime.now().year} $author"
            )
        }
    }
}
