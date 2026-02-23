plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("maven-publish")
}

version = "${property("version_suffix")}+v${property("mod_version")}"
group = property("maven_group") as String

val modVersion = property("mod_version") as String

base {
    archivesName = property("archives_base_name") as String
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("pay-everyone") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runs {
        named("client") {
            client()
            configName = "Pay Everyone Client"
            ideConfigGenerated(true)  // INFRA-06: IDEA run configs
            runDir = "run"
        }
    }
}

// Source sets configuration
// - Root project uses source files directly from src/
// - Versioned projects use Stonecutter preprocessed files from build/generated/stonecutter/
if (project == rootProject) {
    // Root project (for IDE development) uses source files directly
    sourceSets {
        named("client") {
            java {
                srcDir("src/client/java")
            }
            resources {
                srcDir("src/client/resources")
            }
        }
    }
} else {
    // Versioned projects use only Stonecutter-generated files
    // We must replace the default source directories to avoid duplicates with root project sources
    sourceSets {
        named("client") {
            java {
                setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/client/java")))
            }
        }
    }
    
    // Ensure Stonecutter generates files before any task that uses client sources
    tasks.configureEach {
        if (name == "compileClientJava" || name == "sourcesJar") {
            dependsOn("stonecutterGenerateClient")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings(loom.officialMojangMappings())  // User decision: keep Mojang mappings
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
}

tasks.processResources {
    val mcVersionRange = (findProperty("minecraft_version_range") as? String) ?: ">=1.21"
    inputs.property("version", modVersion)
    inputs.property("minecraft_version_range", mcVersionRange)

    filesMatching("fabric.mod.json") {
        expand(
            "version" to modVersion,
            "minecraft_version_range" to mcVersionRange
        )
    }
}

tasks.withType<JavaCompile> {
    options.release = 21
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}
