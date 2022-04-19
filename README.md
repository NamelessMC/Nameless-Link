# Nameless-Link

The "Nameless Link" Discord bot synchronizes user roles to and from a specific Discord Guild.

For documentation please consult the [wiki](https://github.com/NamelessMC/Nameless-Link/wiki).

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
