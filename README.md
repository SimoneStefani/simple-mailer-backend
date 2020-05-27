# SimpleMailer Backend

| Automation                | Status                                                                                                                                        |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| CD/CI Pipeline (`master`) | ![CD/CI Workflow](https://github.com/SimoneStefani/simple-mailer-backend/workflows/CD/CI%20Workflow/badge.svg)                                |
| Dependency Management     | [![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=SimoneStefani/simple-mailer-backend)](https://dependabot.com) |

## Table of Contents

- [About the Project](#about-the-project)
- [Getting Started](#getting-started)
- [Project Details](#project-details)
  - [Dependencies](#dependencies)
  - [Code Style](#code-style)
  - [Security Analysis](#security-analysis)
  - [Continuous Integration](#continuous-integration)
  - [Continuous Deployment](#continuous-deployment)
  - [Project Architecture](#project-architecture)
  - [Observability](#observability)
- [Further Considerations](#further-considerations)

## About The Project

SimpleMailer Backend application written in [Kotlin](https://kotlinlang.org/) using [Ktor](https://ktor.io/servers/index.html) as framework. This project is built to work as a RESTful API for [SimpleMailer Frontend](https://github.com/SimoneStefani/simple-mailer-frontend)and its main purpose is to manage user authentication and send emails with different services. This application is a proof-of-concept and not expected to work in production.

## Getting Started

To get a local copy of this project up and running follow these steps.

1. Clone the repository:

```sh
git clone git@github.com:SimoneStefani/simple-mailer-backend.git
```

2. Ensure that all the environment variables declared in `resources/application.conf` are available in the development environment.

3. Install all the Gradle dependencies (this project includes a Gradle wrapper):

4. Start the application:

```sh
./gradlew run
```

## Project Details

### Dependencies

The Gradle dependencies are listed in the `build.gradle.kts` while the versions are declared in the `gradle.properties`; This project uses Gradle 6+ hence we try to import BOM files using the _platform_ feature when possible.

### Code Style

This project uses [KTLint](https://ktlint.github.io/) in multiple environments and we suggest you to use it also during development. You can run the basic linting task with `./gradlew ktlintFormat` to lint and format eventual issues.

### Continuous Integration

This project uses GitHub Actions for continuous integration (CI) and deployment. The complete configuration can be found in `.github/workflows/cdci-workflow.yml`. Common steps in the CI process are building and linting. The developer will get promptly notified of failures of CI jobs.

### Continuous Deployment

This project is automatically deployed (CD) to Heroku by GitHub Actions. The deployment pattern is dependent on the Git branches in the way that code merged into a branch triggers a deployment of such branch to a specific environment. While this project currently provides only one environment (prod) this kind setup makes it easier to extend to multiple environments.

| Environment | Git Branch | Heroku App                                                                       |
| ----------- | ---------- | -------------------------------------------------------------------------------- |
| Production  | `master`   | [simple-mailer-backend](https://dashboard.heroku.com/apps/simple-mailer-backend) |

In order to deploy to Heroku GitHub Actions make use of the `HEROKU_API_KEY` which is provided as a [secret](https://help.github.com/en/github/automating-your-workflow-with-github-actions/virtual-environments-for-github-actions#creating-and-using-secrets-encrypted-variables) to the job.

### Project Architecture

The application follows the standard Ktor structure. The entry point is `Application.kt` where we instantiate all services, install all [Ktor features](https://ktor.io/servers/features.html) and declare the routing. The most notable features include CORS handling, authentication with JWT and content negotiation and serialization/deserialization with Gson. The routes classes in `simplemailer/api/` contain the logic for request/response handling and delegate operations to `/mailer` and `persistence` services. The application contains a limited number of tests.

The data is persisted in a PostgreSQL database with connections pooled through Hikari. The mapping between the models (entities) properties and the DB columns is done through [Jetbrains Exposed](https://github.com/JetBrains/Exposed). The DB operations are run asynchronously with the use of [Kotlin Coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html).

The `RedundantMailerService.kt` leverages two different mail services which implement the `MailerService.kt` interface and expose each their own their `send` method and throw IO exception. One of the services is marked as primary and it is used by default while the other is utilized when the primary service fails.

The whole application is eventually built into a fat JAR and run on the web server.

### Observability

Heroku provides a [simple monitoring solution](https://devcenter.heroku.com/categories/monitoring-metrics) which is very valuable for simple projects such this one. On top of this we use [Sentry](https://sentry.io/) for error reporting and [Timber](https://timber.io/) for logs management. Such tools are accessible as Heroku add-ons.

## Further Considerations

- Due to the limited development time this project only few tests have been implemented but more would provide more confidence in working with this codebase.
- More custom exceptions (e.g for email services failure) would be advisable together with more structured HTTP responses.
- A more robust and integrated authentication and authorization solution would be recommended, for example using Bcrypt for password hashing (even if the implemented HMAC512 solution is the one recommended on Ktor website).
- The email service failover strategy could be highly improved for example by implementing some form of temporal locality (i.e. if a service fails use the other for _x_ amount of time since if a service fails once it is likely to file more in the near time).
- Regarding scalability this application should perform quite well since this application runs on Netty webserver and is built with asynchronous execution and non-blocking IO. Th DB connections are pooled which guarantees better performance. The limit is simply the amount of dedicated resources on Heroku and for heavy traffic a solution such deployment on a Kubernetes cluster may be recommended.
