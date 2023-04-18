import com.aliucord.gradle.AliucordExtension
import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.aliucord.com/snapshots")
    }
    dependencies {
        classpath("com.github.Aliucord:gradle:main-SNAPSHOT") {
                 exclude("com.github.js6pak", "jadb")
              }
        classpath("com.aliucord:jadb:1.2.1-SNAPSHOT")
        classpath("com.android.tools.build:gradle:7.0.4")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliucord.com/snapshots")
    }
}

fun Project.aliucord(configuration: AliucordExtension.() -> Unit) = extensions.getByName<AliucordExtension>("aliucord").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "com.aliucord.gradle")


    aliucord {
        author("mantikafasi", 287555395151593473)

        updateUrl.set("https://raw.githubusercontent.com/mantikafasi/AliucordPlugins/builds/updater.json")
        buildUrl.set("https://raw.githubusercontent.com/mantikafasi/AliucordPlugins/builds/%s.zip")
    }

    android {
        compileSdkVersion(30)

        defaultConfig {
            minSdk = 24
            targetSdk = 30
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    dependencies {
        val discord by configurations
        val compileOnly by configurations

        discord("com.discord:discord:aliucord-SNAPSHOT")
        compileOnly("com.aliucord:Aliucord:main-SNAPSHOT")
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
