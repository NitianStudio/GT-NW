import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import de.undercouch.gradle.tasks.download.Download
import java.util.*

buildscript {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            name = "Github Repository"
        }
    }
    dependencies {
        classpath("com.google.code.gson:gson:2.11.0")

    }
}
plugins {
    `java-library`
    id("de.undercouch.download").version("5.6.0")
}

val mcVersion:String by rootProject
val archiveBaseName: String by rootProject
val fabricLoaderVersion: String by rootProject
val fabricApiVersion: String by rootProject



enum class Resources {
    cf,mr,curseforge,modrinth;
}

data class Value(
    @SerializedName("id")
    var id: String,
    @SerializedName("name")
    var name: String,
    @SerializedName("desc")
    var desc: String,
    @SerializedName("needClient")
    var needClient: Boolean,
    @SerializedName("needServer")
    var needServer: Boolean,
    @SerializedName("source")
    var resource: Resources,
    @SerializedName("modVersion")
    var modVerein: String
)
data class ModList(
    @SerializedName("list")
    var list: ArrayList<Value>
)
val gson = GsonBuilder().setPrettyPrinting().create()
var list = gson.fromJson(file("modlist.json").bufferedReader(Charsets.UTF_8), ModList::class.java)
println("modlist:")
list.list.forEach {
    println("\t${it.name}")
}

tasks {
    val downloadMCL = register<Download>("downloadMCL") {
        src(listOf(
            "https://github.com/HMCL-dev/HMCL/releases/download/v3.5.8.249/HMCL-3.5.8.249.jar",
            "https://github.com/MrShieh-X/console-minecraft-launcher/releases/download/2.2.1/cmcl.jar"
        ))
        dest(file("./"))
        overwrite(true)
    }
    val setCMCLConfig = register<Exec>("setCMCLConfug") {
        dependsOn(downloadMCL)
        workingDir(file("./"))
        commandLine(if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) "cmd" else "sh",
            "/c", "java", "-Dfile.encoding=UTF-8", "-jar", "cmcl.jar", "config", "downloadSource", 0)

    }
    val installFabricClient = register<Exec>("installFabricClient") {
        dependsOn(setCMCLConfig)
        workingDir(file("./"))
        commandLine(if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) "cmd" else "sh",
            "/c", "java", "-Dfile.encoding=UTF-8", "-jar", "cmcl.jar", "install", mcVersion,
            "-n", archiveBaseName , "-s", "--fabric=$fabricLoaderVersion", "--api=$fabricApiVersion")
    }

    val downloadLast = register<Exec>("downloadLast") {
        workingDir(file("./"))
    }

    val listTasks = ArrayList<TaskProvider<Exec>>()
    for (value in list.list) {
        val provider = register<Exec>("downloadClientMod${value.name.replace(" ", "")}") {
            setGroup("downloadMods")
            workingDir(file("./"))
            val id = value.id
            val resource = value.resource.name
            val modVerein = value.modVerein
            commandLine(
                if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) "cmd" else "sh",
                "/c", "java", "-Dfile.encoding=UTF-8", "-jar", "cmcl.jar", "mod", "--install",
                "--source=$resource", "--id=$id", "--game-version=$mcVersion", "--version=$modVerein"
            )

        }
        listTasks.add(provider)
    }


    build.configure {
        dependsOn(installFabricClient)
        dependsOn(listTasks)
    }
}