import kotlin.system.exitProcess

plugins {
    java
    application
    `maven-publish`
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

val mainClass = "org.lukkitmc.lukkitclip.Main"

tasks.jar {
    val java6Jar = project(":java6").tasks.named("jar")
    val java17Jar = project(":java21").tasks.named("shadowJar")
    dependsOn(java6Jar, java17Jar)

    from(zipTree(java6Jar.map { it.outputs.files.singleFile }))
    from(zipTree(java17Jar.map { it.outputs.files.singleFile }))

    manifest {
        attributes(
            "Main-Class" to mainClass
        )
    }

    doFirst {
        val clipVerFile = File("lukkitclip-version")
        if (!clipVerFile.exists()) {
            if(!clipVerFile.createNewFile()){
                println("failed to create file: lukkitclip-version")
                exitProcess(1)
            }
        }
        clipVerFile.writeText(project.version.toString())
    }

    from(file("lukkitclip-version")) {
        into("META-INF")
    }

    from(file("LUKKITCLIP_LICENSE")) {
        into("META-INF/license")
        rename { "lukkitclip-LICENSE.txt" }
    }

    rename { name ->
        if (name.endsWith("-LICENSE.txt")) {
            "META-INF/license/$name"
        } else {
            name
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    val java6Sources = project(":java6").tasks.named("sourcesJar")
    val java17Sources = project(":java21").tasks.named("sourcesJar")
    dependsOn(java6Sources, java17Sources)

    from(zipTree(java6Sources.map { it.outputs.files.singleFile }))
    from(zipTree(java17Sources.map { it.outputs.files.singleFile }))

    archiveClassifier.set("sources")
}

val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")

publishing {
    publications {
        register<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            artifact(sourcesJar)
            withoutBuildIdentifier()
        }

        repositories {
            val url = if (isSnapshot) {
                "https://repo.leavesmc.org/snapshots/"
            } else {
                "https://repo.leavesmc.org/releases/"
            }

            maven(url) {
                credentials(PasswordCredentials::class)
                name = "leavesmc"
            }
        }
    }
}

tasks.register("printVersion") {
    doFirst {
        println(version)
    }
}
