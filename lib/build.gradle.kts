/*
 * Copyright (C) 2019 theta4j project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.*

plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
    signing
}

version = "1.0.0"

android {
    compileSdk = 31
    defaultConfig {
        minSdk = 25
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_11)
        targetCompatibility(JavaVersion.VERSION_11)
    }
}

tasks {
    create<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
        group = BasePlugin.BUILD_GROUP

        options.locale = "en_US"
        source = android.sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].java.getSourceFiles()
        classpath += files(android.bootClasspath)
        android.libraryVariants.all {
            classpath += javaCompileProvider.get().classpath
            classpath += files("$buildDir/intermediates/compile_library_classes_jar/$name")
        }
    }
    create<Jar>("sourceJar") {
        group = BasePlugin.BUILD_GROUP

        archiveClassifier.set("sources")
        from(android.sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].java.srcDirs)
    }
    create<Jar>("javadocJar") {
        group = BasePlugin.BUILD_GROUP

        archiveClassifier.set("javadoc")
        val javadoc = project.tasks[JavaPlugin.JAVADOC_TASK_NAME] as Javadoc
        dependsOn(javadoc)
        from(javadoc.destinationDir)
    }
}

afterEvaluate {
    publishing {
        publications.create<MavenPublication>("ossrh") {
            from(components["release"])
            groupId = "org.theta4j"
            artifactId = "theta-code-reader"
            version = project.version as String
            artifact(tasks["sourceJar"])
            artifact(tasks["javadocJar"])
            pom {
                name.set("THETA Code Reader")
                description.set("QR Code reader library for RICOH THETA Plug-in system.")
                url.set("https://github.com/theta4j/theta-web-api")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                    developers {
                        developer {
                            name.set("theta4j Project")
                            email.set("info@theta4j.org")
                        }
                    }
                    scm {
                        url.set("https://github.com/theta4j/theta-code-reader.git")
                    }
                }
            }
        }

        repositories {
            maven {
                url = uri("$buildDir/repo")
            }

            maven {
                val props = Properties().apply {
                    rootProject.file("local.properties").inputStream().use(this::load)
                }
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = props.getProperty("ossrh.user")
                    password = props.getProperty("ossrh.password")
                }
            }
        }
    }

    signing {
        sign(publishing.publications["ossrh"])
    }
}

dependencies {
    api("androidx.annotation:annotation:1.3.0")
    implementation("com.google.zxing:core:3.3.3")
    implementation("com.google.zxing:android-core:3.3.0")
}
