plugins {
    id("java")
}

group = "net.silver"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:7.0.2")
}
