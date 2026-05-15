import com.aliucord.gradle.AliucordExtension
import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.aliucord.com/releases")
        maven("https://maven.aliucord.com/snapshots")


    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.jadb)
        classpath(libs.build.gradle)
    }
}


allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliucord.com/releases")
        // Aliucord/Aliucord isnt in releases
        maven("https://maven.aliucord.com/snapshots")

    }

    apply(plugin = "com.aliucord.plugin")
}

fun Project.aliucord(configuration: AliucordExtension.() -> Unit) = extensions.getByName<AliucordExtension>("aliucord").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {

    apply(plugin = "com.android.library")
    apply(plugin = "com.aliucord.plugin")

    aliucord {

        author("mantikafasi", 287555395151593473)

        updateUrl.set("https://raw.githubusercontent.com/mantikafasi/AliucordPlugins/builds/updater.json")
        buildUrl.set("https://raw.githubusercontent.com/mantikafasi/AliucordPlugins/builds/%s.zip")
    }

    android {
        namespace = "com.aliucord.plugins"
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
        val compileOnly by configurations


        compileOnly("com.discord:discord:126021")
        compileOnly("com.aliucord:Aliucord:2.4.0")
    }
}

tasks.register("clean") {
    delete(rootProject.buildDir)
}
