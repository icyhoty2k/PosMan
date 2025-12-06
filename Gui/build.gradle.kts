import net.silver.buildsrc.BuildMeta

plugins {
    java
}


group = "net.silver.gui"
version = project.version

dependencies {

    implementation(BuildMeta.Libs.BASE_JAVA_FX)
    implementation(BuildMeta.Libs.CONTROLS_JAVA_FX)
    implementation(BuildMeta.Libs.FXML_JAVA_FX)
    implementation(BuildMeta.Libs.GRAPHICS_JAVA_FX)

    implementation(project(":Logging"))
    implementation(project(":Utils"))
    implementation(project(":Persistence"))
    implementation(project(":Resources"))
    implementation(project(":Config"))
}

tasks.jar {
    archiveBaseName.set(name)
    archiveVersion.set("$version")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = BuildMeta.MAIN_CLASS
    }
}

tasks.test {
    useJUnitPlatform()
}
