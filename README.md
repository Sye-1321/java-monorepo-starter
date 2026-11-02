# Java monorepo starter

A small Gradle multi-project Java application. The `core` library analyzes text, and the `app` command-line application reads a UTF-8 file and prints its line, word, and Unicode code-point counts.

## Requirements

- Java 21 or newer
- No separate Gradle installation; the included Gradle 9.8.2 wrapper is used

The build produces Java 21-compatible bytecode even when run with a newer JDK.

## Build and test

On Linux or macOS:

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

`build` compiles both projects, runs all tests, and creates the CLI distributions under `app/build/distributions`.

To run only the tests, use `./gradlew test` or `.\gradlew.bat test`.

## Adapt the template

After creating a repository from the template:

1. Set the project name in `settings.gradle.kts`.
2. Move the Java sources and tests into directories matching your package names, then update their `package` declarations and imports.
3. Rename `Main.java` and `MainTest.java` if desired, keeping each filename aligned with its class name.
4. In `app/build.gradle.kts`, set `application.mainClass` to the fully qualified entry-point class and set `application.applicationName` to the desired executable name.
5. Update the executable name in the usage message in the entry-point class and in the commands below.

Run `./gradlew clean build` (or `.\gradlew.bat clean build` on Windows) after renaming to catch stale package, class, and path references.

## Run

Run the CLI directly through Gradle:

```shell
./gradlew :app:run --args="../README.md"
```

On Windows, replace `./gradlew` with `.\gradlew.bat`. Relative arguments passed to `:app:run` are resolved from the `app` project directory; use an absolute path or prefix a repository-root path with `../`.

To install a local runnable distribution:

```shell
./gradlew :app:installDist
./app/build/install/text-analyzer/bin/text-analyzer path/to/file.txt
```

The installed Windows launcher is `app\build\install\text-analyzer\bin\text-analyzer.bat`.

The `app` project has a Gradle project dependency on `core`; all counting behavior lives in `core` and is reused by the CLI.
