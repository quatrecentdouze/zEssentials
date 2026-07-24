import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    id("io.papermc.paperweight.userdev")
}

group = "NMS:V26_1_2"

dependencies {
    compileOnly(project(":API"))
    paperweight.paperDevBundle("26.1.2.build.+")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.compileJava {
    options.release = 21
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}
