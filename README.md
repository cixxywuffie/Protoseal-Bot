# ProtoSeal

ProtoSeal is a consent-focused Discord bot for restraint-themed roleplay. It is built with Java, Spring Boot and Discord4J, and uses slash commands to manage restraints, locks, consent preferences and safety actions.

Created by [CixtroWolf](https://bsky.app/profile/bsky.cixtrowolf.com).

> [!IMPORTANT]
> ProtoSeal is intended for consenting adults and fictional roleplay. Server rules, moderation and clear communication between participants remain essential. `/safeword` is always available to clear a user's restraint state.

## Features

- Fourteen persistent restraint and roleplay-state categories, many with expanded MuzzledFox-inspired types.
- Global locks using padlocks, glue, sewing, tape, fixed-duration timelocks or an irreversible permalock.
- Active timelocks expire automatically and survive bot restarts.
- Permalocks and active timelocks can only be cleared early through `/safeword`.
- Newly applied restraints inherit an active lock.
- Active mitts prevent applying, changing or removing any other restraint until the mitts are removed.
- Per-server consent preferences.
- Owner invitations that must be accepted by the invited user through DM.
- Administrator-only consent reset tools.
- Per-server blacklist for disabling the bot in selected NSFW channels.
- Persistent state using H2 locally and MariaDB in containerized environments.
- Discord embeds for restraint status.

## Consent modes

| Mode | Behaviour |
| --- | --- |
| `self_only` | Only the user can manage their own restraints. This is the default. |
| `ask` | Other members may request restraint, lock and timelock changes; the user must accept each request before it is applied. |
| `exposed` | Other server members can apply restraints and locks to the user. |
| `owner` | Only an owner explicitly invited and accepted by the user can manage their restraints. |
| `disabled` | Restraint interactions are disabled. The user can still use `/safeword`. |

Consent is scoped to each Discord server. Administrators can use `/consentreset` to restore `self_only` if moderation intervention is required.

## Commands

### Restraints

| Command | Parameters | Purpose |
| --- | --- | --- |
| `/armcuffs` | `target`, `type` | Change arm restraints, including armbinders, cuffs, belts and restrictive arm positions. |
| `/legcuffs` | `target`, `type` | Change leg restraints, including cuffs, hobbles, spreader bars and hogties. |
| `/gag` | `target`, `type` | Apply or remove ball gags, bit gags, muzzles and other gag types. |
| `/hood` | `target`, `type` | Apply or remove deprivation, bondage, animal-themed and other hoods. |
| `/straitjacket` | `target`, `type` | Change straitjacket straps and sleeve configurations. |
| `/suits` | `target`, `type` | Apply or remove latex, plush, animal, drone, sack and other restraint suits. |
| `/mitts` | `target`, `type` | Apply mittens, puppy paws, horse or cow hooves, and ducky mitts. |
| `/chastity` | `target`, `type` | Apply a cage, belt, null-bulge or udder-style restraint. |
| `/blindfold` | `target`, `type` | Apply leather, bandage, goggles, paneled or opaque-contact blindfolds. |
| `/collar` | `target`, `type` | Apply or remove leather, latex, rubber, chain and iron collars. |
| `/confine` | `target`, `type` | Confine a user in a cell, padded room, sack, circle or pit. |
| `/encase` | `target`, `type` | Encase a user as a mummy or in a gibbet, rubber, glass, cage, vacbed, cement and other forms. |
| `/nametag` | `target`, `label` | Set a label shown only in `/rdstatus`; omit `label` to remove it. This never changes the Discord nickname. |
| `/leash` | `target`, `action` | Attach a leash held by the command user or remove the active leash. |

### Management and safety

| Command | Purpose |
| --- | --- |
| `/about` | Show information about ProtoSeal, its author and source code. |
| `/donate` | Show the configured page for supporting ProtoSeal development. |
| `/lock` | Apply or remove a lock across all active restraints. |
| `/timelock` | Lock all of a user's active restraints for 1 minute to 30 days. |
| `/rdstatus` | Display a user's consent, active restraints and lock status. |
| `/consent` | Configure the user's consent mode. |
| `/safeword` | Clear the user's restraint state for the current server. |
| `/help` | Show public commands. |
| `/adminhelp` | Show administrator commands. |
| `/consentreset` | Reset a user's consent to `self_only` (administrator only). |
| `/channelconfig` | Configure allowed bot channels (administrator only). |

ProtoSeal allows commands only in Discord text channels marked as age-restricted (NSFW).
Administrators can use `/channelconfig block` and `unblock` to maintain a blacklist of NSFW channels
where the bot must remain unavailable. `/channelconfig list` shows that blacklist and `clear` removes
it. The NSFW status is checked dynamically, so changing it in Discord immediately updates the filter.
`/channelconfig` and the safety command `/safeword` remain available outside NSFW channels so that
configuration and safety actions cannot become unreachable.

### Donations

The `/donate` command displays an optional HTTPS donation page configured by the instance operator.
Optionally set `DONATION_URL` before starting ProtoSeal:

```powershell
$env:DONATION_URL="https://ko-fi.com/your-page"
```

For Compose deployments, set `DONATION_URL` in the `.env` file. Donations are optional, help cover
hosting and development costs, and do not grant any additional bot permissions or features.

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

Clone the repository and set the token in your shell. When no active profile is supplied, Spring uses
the `local` profile and stores data in the file-based H2 database.

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

### Environment configuration

Docker Compose injects deployment values from `.env` into the ProtoSeal container. Copy `.env.example`
to `.env` and keep the resulting file outside version control. Compose requires `BOT_TOKEN`, `DB_NAME`,
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `DB_ROOT_PASSWORD`; it does not provide fallback credentials.
`DB_URL` uses `mariadb` as its host because that is the database service name on the internal Compose
network. If `DB_NAME` changes, update the database name at the end of `DB_URL` as well.

| Variable | Default | Purpose |
| --- | --- | --- |
| `BOT_TOKEN` | Required | Discord bot token. |
| `DONATION_URL` | Empty | Optional HTTPS donation page used by `/donate`. |
| `APP_NAME` | `ProtoSeal` | Spring application name. |
| `SPRING_PROFILES_ACTIVE` | `development` | Active Spring profile. |
| `DB_NAME` | Required by Compose | MariaDB database created by Compose. |
| `DB_URL` | Required by Compose | JDBC URL; the example uses `jdbc:mariadb://mariadb:3306/protoseal_dev`. |
| `DB_USERNAME` | Required by Compose | Application and MariaDB username. |
| `DB_PASSWORD` | Required by Compose | Application database password. |
| `DB_ROOT_PASSWORD` | Required by Compose | MariaDB administrative password. |
| `JPA_DDL_AUTO` | `update` | Hibernate schema policy for development. |
| `HIBERNATE_JDBC_TIME_ZONE` | `UTC` | JDBC time zone used by Hibernate. |
| `APP_LOG_LEVEL` | `INFO` | ProtoSeal application log level. |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75.0` | JVM options applied inside the bot container. |

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
| `local` (default) | Local file-based H2 | IntelliJ and individual local development |
| `development` | MariaDB | Shared development server or Compose |
| `production` | MariaDB | Production container with externally supplied secrets |

For production, run the image with `SPRING_PROFILES_ACTIVE=production` and provide at least `BOT_TOKEN`
and `DB_PASSWORD`. By default, the profile connects to the `mariadb` service from Compose using the
`protoseal_dev` database and `protoseal` user. Set `DB_URL` and, when needed, `DB_USERNAME` in `.env`
to use an external MariaDB instance instead. The production profile currently defaults `JPA_DDL_AUTO`
to `update`; move to versioned migrations and set it to `validate` before a public launch.

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

## CI/CD with GitHub Actions

The public CI workflow in `.github/workflows/tests.yml` runs the Maven test suite for pull requests and
pushes to `main`. It has read-only repository permissions and receives no deployment secrets, making it
safe for contributions to a public repository.

After the tests for a push to `main` complete successfully, `.github/workflows/deploy.yml` connects to
the development server over SSH, fast-forwards its checkout and rebuilds the Compose services. It can
also be started manually from the Actions tab. The server's `.env` file and the `mariadb-data` volume
are not replaced.

Before enabling deployment:

1. On the server, clone the repository, create its development `.env`, and verify that
   `docker compose up --build -d` works for the deployment user.
2. Give that user read access to the GitHub repository and permission to run Docker without an
   interactive password prompt.
3. Create a GitHub environment named `development` under **Settings > Environments**. Optional approval
   rules can be added there.
4. Add these environment secrets:

| Secret | Value |
| --- | --- |
| `DEPLOY_HOST` | Server hostname or IP address. |
| `DEPLOY_PORT` | SSH port; use `22` for the default. |
| `DEPLOY_USER` | Restricted server user that owns the checkout. |
| `DEPLOY_PATH` | Absolute path to the repository on the server. |
| `SSH_PRIVATE_KEY` | Private key dedicated to GitHub Actions. Add its public key to the server user's `~/.ssh/authorized_keys`. |
| `SSH_KNOWN_HOSTS` | Verified server host-key line, obtained from a trusted machine with `ssh-keyscan -H -p PORT HOST`. |

Keep the server checkout on `main` with no local code changes. The deployment intentionally uses
`git pull --ff-only`, so it stops instead of overwriting unexpected server-side modifications. A failed
test prevents deployment, and concurrent deployments are serialized. If the server-side Docker build
fails, Compose leaves the currently running containers in place and reports the failed deployment.

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
