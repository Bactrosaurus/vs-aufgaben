plugins {
    id("java")
}

group = "de.uulm.in.vs.grn"
version = "1.0"

repositories {
    mavenCentral()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.uulm.in.vs.grn.vnscp.server.Main"
    }
}