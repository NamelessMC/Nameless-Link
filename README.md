# Nameless-Link

The "Nameless Link" Discord bot synchronizes user roles to and from a specific Discord Guild.

For documentation please consult the [wiki](https://github.com/NamelessMC/Nameless-Link/wiki).

## Configuration

### Bot Activity/Presence

You can customize the bot's activity status using environment variables:

- `BOT_ACTIVITY_TYPE` - The type of activity to display. Valid options:
  - `PLAYING` - Shows "Playing {message}"
  - `LISTENING` - Shows "Listening to {message}"
  - `WATCHING` - Shows "Watching {message}"
  - `COMPETING` - Shows "Competing in {message}"
  - `STREAMING` - Shows "Streaming {message}" with a purple "LIVE" indicator (requires `BOT_ACTIVITY_URL`)
  - `CUSTOM` - Shows a custom status message
- `BOT_ACTIVITY_MESSAGE` - The message to display in the activity status
- `BOT_ACTIVITY_URL` - (Optional, required for `STREAMING`) The URL for the stream (e.g., Twitch/YouTube URL)

**Example (docker-compose.yaml):**
```yaml
environment:
  BOT_ACTIVITY_TYPE: PLAYING
  BOT_ACTIVITY_MESSAGE: with NamelessMC
```

**Example (STREAMING):**
```yaml
environment:
  BOT_ACTIVITY_TYPE: STREAMING
  BOT_ACTIVITY_MESSAGE: NamelessMC Development
  BOT_ACTIVITY_URL: https://www.twitch.tv/your_channel
```

## Translations

<a href="https://translate.namelessmc.com/engage/namelessmc/">
<img src="https://translate.namelessmc.com/widgets/namelessmc/-/discord-bot/multi-auto.svg" alt="Translation status" />
</a>

## Compiling

Requirements: Maven, JDK 11, JDK 17, git

`apt install maven openjdk-11-jdk openjdk-17-jdk git`

```sh
git clone https://github.com/NamelessMC/Nameless-Java-API
cd Nameless-Java-API
# You may need to manually set JAVA_HOME to your JDK 11 installation
mvn install
cd ..

git clone https://github.com/NamelessMC/Nameless-Link
cd Nameless-Link
# You may need to manually set JAVA_HOME to your JDK 17 installation
mvn package
cd target
# find jar file here
```
