plugins {
    `java-library`
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
