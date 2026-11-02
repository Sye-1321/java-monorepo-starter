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

## Run

Run the CLI directly through Gradle:

```shell
./gradlew :app:run --args="path/to/file.txt"
```

On Windows, replace `./gradlew` with `.\gradlew.bat`.

To install a local runnable distribution:

```shell
./gradlew :app:installDist
./app/build/install/text-analyzer/bin/text-analyzer path/to/file.txt
```

The installed Windows launcher is `app\build\install\text-analyzer\bin\text-analyzer.bat`.

The `app` project has a Gradle project dependency on `core`; all counting behavior lives in `core` and is reused by the CLI.
