plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT" apply false
}

stonecutter active "1.21.4"  // Initial active version matches vcsVersion

stonecutter parameters {
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}
