plugins {
    id("java")
}

group = "net.silver"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation(project(":Logging"))
    implementation(project(":Resources"))

}
