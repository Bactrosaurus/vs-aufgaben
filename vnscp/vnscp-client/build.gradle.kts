plugins {
    java
}

dependencies {
    implementation(project(":vnscp-common"))
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "de.uulm.in.vs.grn.vnscp.client.VNSCPClient"
        }
    }
}