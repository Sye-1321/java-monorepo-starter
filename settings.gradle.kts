rootProject.name = "java-monorepo-starter"

include("app", "core")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
