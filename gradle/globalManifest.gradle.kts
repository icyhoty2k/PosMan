import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.api.tasks.bundling.Jar
import java.lang.management.ManagementFactory

subprojects {
    val author = "Ivan Hristov Yanev"

    // Ensure the task is a Jar task before configuring the manifest
    tasks.withType<Jar>().configureEach {
        // Calculate the timestamp when the Jar task is configured
        val now = LocalDateTime.now()
        val timestamp = now.format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )

        // Get network hostname
        val hostname = InetAddress.getLocalHost().hostName

        // Get raw time in milliseconds since epoch
        val buildTimeMillis = System.currentTimeMillis()
        // Determine if the project version is a snapshot
        val isSnapshot = project.version.toString().endsWith("-SNAPSHOT", ignoreCase = true)
        manifest {
            attributes(
                // ===== Project Info =====
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to author,
                "Implementation-URL" to "https://github.com/icyhoty2k",

                // ===== Build Info (Highly Detailed) =====
                "Built-Timestamp" to timestamp,
                "Built-Time-Millis" to buildTimeMillis, // New: Machine-readable time
                "Built-By" to "computerUserName -> " + System.getProperty("user.name"),
                "Build-Hostname" to hostname, // New: Hostname of the build machine

                // Detailed OS Information: Name/Architecture v.Version
                "Build-OS" to System.getProperty("os.name") + "/" + System.getProperty("os.arch") + " v." + System.getProperty(
                    "os.version"
                ),
                "Build-OS-Data-Model" to System.getProperty("sun.arch.data.model"),

                // Detailed Java Information: Vendor Version/VM_Version
                "Build-Java-Version" to System.getProperty("java.vendor") + " " + System.getProperty("java.version") + "/" + System.getProperty(
                    "java.vm.version"
                ),
                "Build-JVM-Name" to ManagementFactory.getRuntimeMXBean().vmName,
                "Build-Gradle-Version" to gradle.gradleVersion,
                "Build-Source-Encoding" to System.getProperty("file.encoding"), // New: Source file encoding
                "Build-Is-Snapshot" to isSnapshot, // New: Flag for snapshot version
                // ===== Legal & Custom =====
                "Author" to author,
                "License" to "MIT",
                // Calculate copyright year dynamically
                "Copyright" to "© ${now.year} $author"
            )
        }
    }
}
