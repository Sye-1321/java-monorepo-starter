# Java monorepo starter

A small Gradle multi-project Java backend. The `libs/core` library analyzes text, `apps/cli` reads a UTF-8 file and prints its line, word, and Unicode code-point counts, and `apps/api` exposes the same analysis over HTTP.

## Requirements

- Java 21 or newer
- Docker for PostgreSQL and integration tests
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

`build` compiles all projects, runs all tests, creates the CLI distributions under `apps/cli/build/distributions`, and packages an executable API jar under `apps/api/build/libs`.

To run only the tests, use `./gradlew test` or `.\gradlew.bat test`.

## Adapt the template

After creating a repository from the template:

1. Set the project name in `settings.gradle.kts`.
2. Move the Java sources and tests into directories matching your package names, then update their `package` declarations and imports.
3. Rename `Main.java` and `MainTest.java` if desired, keeping each filename aligned with its class name.
4. In `apps/cli/build.gradle.kts`, set `application.mainClass` to the fully qualified entry-point class and set `application.applicationName` to the desired executable name.
5. Update the executable name in the usage message in the entry-point class and in the commands below.

Run `./gradlew clean build` (or `.\gradlew.bat clean build` on Windows) after renaming to catch stale package, class, and path references.

## Run

Start PostgreSQL with a local password:

```shell
export POSTGRES_PASSWORD=local-password
docker compose up -d
```

On PowerShell, set the password with `$env:POSTGRES_PASSWORD = "local-password"` before running `docker compose up -d`.

Configure and run the API:

```shell
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/analyses
export SPRING_DATASOURCE_USERNAME=analyses
export SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"
./gradlew :apps:api:bootRun
```

On PowerShell, set the equivalent `SPRING_DATASOURCE_*` environment variables and use `.\gradlew.bat`. Flyway applies the database migration when the API starts.

Then submit text for analysis:

```shell
curl -i -X POST http://localhost:8080/api/v1/analyses -H "Content-Type: application/json" -d '{"text":"Hello world"}'
```

Retrieve the stored analysis using the UUID from the response or `Location` header:

```shell
curl http://localhost:8080/api/v1/analyses/<analysis-id>
```

Run the CLI directly through Gradle:

```shell
./gradlew :apps:cli:run --args="../../README.md"
```

On Windows, replace `./gradlew` with `.\gradlew.bat`. Relative arguments passed to `:apps:cli:run` are resolved from the `apps/cli` project directory; use an absolute path or prefix a repository-root path with `../../`.

To install a local runnable distribution:

```shell
./gradlew :apps:cli:installDist
./apps/cli/build/install/text-analyzer/bin/text-analyzer path/to/file.txt
```

The installed Windows launcher is `apps\cli\build\install\text-analyzer\bin\text-analyzer.bat`.

Both applications have a Gradle project dependency on `libs/core`; all counting behavior lives in `libs/core` and is reused by the API and CLI. The core library has no Spring dependencies.
