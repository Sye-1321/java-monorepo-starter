rootProject.name = "java-monorepo-starter"

include("apps:api", "apps:cli", "libs:core")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
