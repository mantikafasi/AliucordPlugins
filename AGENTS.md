# Agent Notes

## Building Plugins

This repo is an Aliucord plugin workspace. Build any plugin zip by replacing `PluginName` with the module directory name:

```bash
env JAVA_HOME=/tmp/codex-jdk/jdk-21 \
  ANDROID_HOME=/mnt/c/Users/manti/AppData/Local/Android/Sdk \
  ANDROID_SDK_ROOT=/mnt/c/Users/manti/AppData/Local/Android/Sdk \
  bash -c 'tr -d "\r" < ./gradlew | bash -s -- --no-daemon -Dorg.gradle.java.home=/tmp/codex-jdk/jdk-21 :PluginName:make'
```

Successful output is written to:

```text
PluginName/build/outputs/PluginName.zip
```

For example, to build `PasswordLogin`, use `:PasswordLogin:make`.

## Deploying Plugins

The intended deploy task is:

```bash
env JAVA_HOME=/tmp/codex-jdk/jdk-21 \
  ANDROID_HOME=/mnt/c/Users/manti/AppData/Local/Android/Sdk \
  ANDROID_SDK_ROOT=/mnt/c/Users/manti/AppData/Local/Android/Sdk \
  bash -c 'tr -d "\r" < ./gradlew | bash -s -- --no-daemon -Dorg.gradle.java.home=/tmp/codex-jdk/jdk-21 :PluginName:deployWithAdb'
```

For example, to deploy `PasswordLogin`, use `:PasswordLogin:deployWithAdb`.

Current environment caveat: the Android SDK path contains `adb.exe` only, while the Gradle task expects a Linux executable at `platform-tools/adb`. From this WSL shell, Windows executables are not runnable, so `deployWithAdb` fails at `adb devices` unless a usable Linux `adb` is installed or the environment can execute Windows binaries.
