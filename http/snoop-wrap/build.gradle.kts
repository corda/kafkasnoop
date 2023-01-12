
plugins {
    id("kafkasnoop.kotlin-http-service-conventions")
    id("com.google.cloud.tools.jib")
}

repositories {
    mavenLocal()
    mavenCentral()

    val artifactoryContextUrl = property("artifactoryContextUrl")
    maven {
        url = uri("$artifactoryContextUrl/corda-dependencies")
    }

    maven {
        url = uri("$artifactoryContextUrl/corda-os-maven")
        authentication {
            create<BasicAuthentication>("basic")
        }
        credentials {
            username = System.getenv("CORDA_ARTIFACTORY_USERNAME")
            password = System.getenv("CORDA_ARTIFACTORY_PASSWORD")
        }
    }
}

val cordaApiDependencies: Configuration by configurations.creating
cordaApiDependencies.isCanBeConsumed = false
configurations.add(cordaApiDependencies)

configurations {
    compileClasspath.get().extendsFrom(cordaApiDependencies)
}

dependencies {
    implementation(project(":http:serialisation:avro"))
    implementation(project(":http:snoop"))
    implementation(project(":lib:avro"))
    implementation(project(":lib:http"))


    val cordaApiVersion = property("cordaApiVersion")
    cordaApiDependencies(platform("net.corda:corda-api:$cordaApiVersion"))
    cordaApiDependencies("net.corda:corda-avro-schema:$cordaApiVersion")
}

jib {
    container {
        mainClass = "kafkasnoop.wrap.AppKt"

        ports = listOf("9000", "9000")
    }
    extraDirectories {
        paths {
            path {
                setFrom(file("$rootDir/schemas"))
                into = "/schemas"
            }
        }
    }
    to {
        image = "qa-docker-dev.software.r3.com/kafkasnoop-wrap"
        tags = setOf(project.version.toString(), "latest")
        auth {
            username = System.getenv("CORDA_ARTIFACTORY_USERNAME")
            password = System.getenv("CORDA_ARTIFACTORY_PASSWORD")

        }
    }
}

tasks.jar {
    this.archiveBaseName.set("kafkasnoop-wrap")
}

application {
    // Define the main class for the application.
    mainClass.set("kafkasnoop.wrap.AppKt")
}

val copyAvroSchemaDependencies by tasks.registering(Copy::class) {
    val jarToCopyFrom = configurations["cordaApiDependencies"]
        .filter { it.name.endsWith("jar") }
        .filter { it.name.contains("corda-avro-schema") }
        .map { zipTree(it) }
    from(jarToCopyFrom)
    include("**/*.avsc")
    includeEmptyDirs = false
    into("$rootDir/schemas")
}

