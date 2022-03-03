plugins {
    id("java")
}

group = "net.minestom"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    implementation("com.github.Minestom:Minestom:f7d44c4774")
    implementation("io.github.spair:imgui-java-app:1.86.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}