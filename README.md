# ProtoSeal

ProtoSeal is a consent-focused Discord bot for restraint-themed roleplay. It is built with Java, Spring Boot and Discord4J, and uses slash commands to manage restraints, locks, consent preferences and safety actions.

Created by [CixtroWolf](https://bsky.app/profile/cixtrowolf.com).

> [!IMPORTANT]
> ProtoSeal is intended for consenting adults and fictional roleplay. Server rules, moderation and clear communication between participants remain essential. `/safeword` is always available to clear a user's restraint state.

## Features

- Nine restraint categories with multiple levels.
- Global locks using padlocks, glue, sewing, tape or an irreversible permalock.
- Permalocks can only be cleared through `/safeword`.
- Newly applied restraints inherit an active lock.
- Active mitts prevent applying, changing or removing any other restraint until the mitts are removed.
- Per-server consent preferences.
- Owner invitations that must be accepted by the invited user through DM.
- Administrator-only consent reset tools.
- Per-server allowlist for the channels where the bot can be used.
- Persistent state using H2 locally and MariaDB in containerized environments.
- Discord embeds for restraint status.

## Consent modes

| Mode | Behaviour |
| --- | --- |
| `self_only` | Only the user can manage their own restraints. This is the default. |
| `exposed` | Other server members can apply restraints and locks to the user. |
| `owner` | Only an owner explicitly invited and accepted by the user can manage their restraints. |
| `disabled` | Restraint interactions are disabled. The user can still use `/safeword`. |

Consent is scoped to each Discord server. Administrators can use `/consentreset` to restore `self_only` if moderation intervention is required.

## Commands

### Restraints

| Command | Purpose |
| --- | --- |
| `/armcuffs` | Change arm restraints. |
| `/legcuffs` | Change leg restraints. |
| `/gag` | Change the active gag. |
| `/hood` | Change the active hood. |
| `/straitjacket` | Change the straitjacket. |
| `/suits` | Change the restraint suit. |
| `/mitts` | Apply mittens, puppy paws or horse hooves. |
| `/chastity` | Apply a cage, belt or null bulge. |
| `/blindfold` | Apply a leather blindfold, bandage or protection goggles. |

### Management and safety

| Command | Purpose |
| --- | --- |
| `/about` | Show information about ProtoSeal, its author and source code. |
| `/lock` | Apply or remove a lock across all active restraints. |
| `/rdstatus` | Display a user's consent, active restraints and lock status. |
| `/consent` | Configure the user's consent mode. |
| `/safeword` | Clear the user's restraint state for the current server. |
| `/help` | Show public commands. |
| `/adminhelp` | Show administrator commands. |
| `/consentreset` | Reset a user's consent to `self_only` (administrator only). |
| `/channelconfig` | Configure allowed bot channels (administrator only). |

ProtoSeal automatically allows every Discord channel marked as age-restricted (NSFW).
Administrators can use `/channelconfig allow` and `unallow` to maintain a whitelist of additional
channels, and `block` or `unblock` to maintain a blacklist. The blacklist takes priority, including
over NSFW channels. `/channelconfig list` shows both lists and `clear` resets them to the default
NSFW-only behaviour. The NSFW status is checked dynamically, so changing it in Discord immediately
updates the filter. `/channelconfig` and the safety command `/safeword` remain available outside it.

## Requirements

- Java 21 for direct execution, or Podman with Compose
- A Discord application and bot token
- Maven is optional because the Maven Wrapper is included

## Discord setup

1. Create an application in the [Discord Developer Portal](https://discord.com/developers/applications).
2. Add a bot to the application and generate a token.
3. Invite the bot to your server with the `bot` and `applications.commands` scopes.
4. Give it permission to send messages, embeds and direct messages required by your server configuration.

Never commit the bot token to Git. ProtoSeal reads it from the `BOT_TOKEN` environment variable.

## Running locally

Clone the repository and set the token in your shell.

PowerShell:

```powershell
$env:BOT_TOKEN="your-discord-bot-token"
.\mvnw.cmd spring-boot:run
```

Linux or macOS:

```bash
export BOT_TOKEN="your-discord-bot-token"
./mvnw spring-boot:run
```

Slash-command manifests from `src/main/resources/commands` are registered globally when the bot starts. Discord may take some time to propagate global command changes.

## Local database

Development data is stored in an H2 database under `data/`. This directory is ignored by Git, so local test data is not uploaded to the repository.

The current configuration is intended for development. A production deployment should use an external database, backups, restricted credentials and schema migrations.

## Podman and MariaDB

The default Compose stack runs ProtoSeal with the `development` Spring profile and a MariaDB container. The image is OCI-compatible, so the same files can also be used with Docker on a future server.

On Windows, initialize the Podman virtual machine once and start it before using containers:

```powershell
podman machine init
podman machine start
```

If a Podman machine already exists, only `podman machine start` is required.

```bash
cp .env.example .env
# Edit .env and add the Discord token and database passwords.
podman compose up --build -d
podman compose logs -f protoseal
```

Stop the stack with `podman compose down`. MariaDB data remains in the `mariadb-data` named volume. Use `podman compose down -v` only when you intentionally want to delete that database.

The image can also be built without Compose:

```bash
podman build -t protoseal:dev .
```

Available Spring profiles:

| Profile | Database | Intended use |
| --- | --- | --- |
| default | Local file-based H2 | IDE and individual local development |
| `development` | MariaDB | Shared development server or Compose |
| `production` | MariaDB | Production container with externally supplied secrets |

For production, run the image with `SPRING_PROFILES_ACTIVE=production` and provide `BOT_TOKEN`, `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. The production profile currently defaults `JPA_DDL_AUTO` to `update`; move to versioned migrations and set it to `validate` before a public launch.

## Tests

Run the complete test suite with:

```powershell
.\mvnw.cmd test
```

On Linux or macOS:

```bash
./mvnw test
```

The suite covers consent authorization, owner invitations, lock propagation, restraint persistence, database conversions and Discord command manifests.

## Logging

ProtoSeal writes structured command lifecycle and domain events to standard output. The default application log level is `INFO`; set `APP_LOG_LEVEL=DEBUG` to include command reception and status-query diagnostics. Logs include Discord identifiers and outcomes for correlation, but never owner-invitation tokens or message contents.

## Project structure

```text
src/main/java/com/cixtrowolf/protoseal/
├── commands/       Slash-command handlers
├── listeners/      Discord interaction listeners and command registration
├── model/          Restraint domain model
└── persistence/    Consent and restraint persistence modules

src/main/resources/
├── commands/       Discord slash-command manifests
└── application.properties
```

## Production status

ProtoSeal is currently under active development. Before operating a public instance, add production database configuration, secret management, monitoring, backups, rate-limit handling and a documented moderation policy.

## License

ProtoSeal is licensed under the [GNU Affero General Public License v3.0](LICENSE). If you modify the bot and make it available to users over a network, the AGPL requires that those users can obtain the corresponding source code under the same license.
