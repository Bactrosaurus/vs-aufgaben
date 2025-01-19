plugins {
    java
}

repositories {
    mavenCentral()
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "de.uulm.in.vs.grn.vnscp.client.Main"
        }
    }
}