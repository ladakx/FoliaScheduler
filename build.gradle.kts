plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.cjcrafter"
version = "0.7.4-ladakx-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
}

dependencies {
    // 1.12.2 is the oldest version we plan on officially supporting
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2")

    // Remapping classes in paper 1.20.5+
    implementation("xyz.jpenilla:reflection-remapper:0.1.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(8)
    }

    shadowJar {
        archiveFileName.set("FoliaScheduler-$version.jar")
        archiveClassifier.set("")

        dependsOn(":folia:jar", ":spigot:jar")
        from(zipTree(project(":spigot").tasks.jar.get().archiveFile)) {
            exclude("META-INF/**")
        }
        from(zipTree(project(":folia").tasks.jar.get().archiveFile)) {
            exclude("META-INF/**")
        }

        relocate("xyz.jpenilla.reflectionremapper", "com.cjcrafter.foliascheduler.reflectionremapper")
        relocate("net.fabricmc.mappingio", "com.cjcrafter.foliascheduler.mappingio")
    }

    javadoc {
        options {
            this as StandardJavadocDocletOptions
            // suppress warnings for missing Javadoc comments
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("encoding", "UTF-8")
        }
        source(sourceSets.main.get().allJava)
        classpath = sourceSets.main.get().compileClasspath
    }

    named<Jar>("sourcesJar") {
        dependsOn(":folia:jar", ":spigot:jar")

        from(sourceSets.main.get().allSource)
        //from(project(":spigot").sourceSets.main.get().allSource)
        //from(project(":folia").sourceSets.main.get().allSource)
    }

    test {
        useJUnitPlatform()
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    }
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            // Use the 'shadow' component for publishing
            from(components["shadow"])
            artifact(tasks.named<Jar>("sourcesJar").get())
            artifact(tasks.named<Jar>("javadocJar").get())

            groupId = "com.cjcrafter"
            artifactId = "foliascheduler"
            version = project.version.toString()

            pom {
                name.set("FoliaScheduler")
                description.set("Task scheduler for Spigot and Folia plugins")
                url.set("https://github.com/CJCrafter/FoliaScheduler")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("CJCrafter")
                        name.set("Collin Barber")
                        email.set("collinjbarber@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/CJCrafter/FoliaScheduler.git")
                    developerConnection.set("scm:git:ssh://github.com/CJCrafter/FoliaScheduler.git")
                    url.set("https://github.com/CJCrafter/FoliaScheduler")
                }
            }
        }
    }

    // Deploy this repository locally for staging, then let the root project actually
    // upload the maven repo using jReleaser
    repositories {
        maven {
            name = "localBuildRepo"
            url = layout.buildDirectory.dir("local-maven-repo").map { it.asFile.toURI() }.get()
        }
    }
}
