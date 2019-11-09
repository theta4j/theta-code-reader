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

val sourceJarTaskName = "sourceJar"
val javadocJarTaskName = "javadocJar"

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    `maven-publish`
    id("digital.wup.android-maven-publish") version "3.6.2"
    id("com.jfrog.bintray") version "1.8.4"
}

android {
    compileSdkVersion(29)
    defaultConfig {
        minSdkVersion(25)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes["release"].apply {
        isMinifyEnabled = false
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}

tasks {
    create<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
        group = BasePlugin.BUILD_GROUP

        options.locale = "en_US"
        source = android.sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].java.sourceFiles
        classpath += files(android.bootClasspath)
        android.libraryVariants.all {
            classpath += javaCompileProvider.get().classpath
            classpath += files(renderscriptCompileProvider.get().objOutputDir)
        }
    }
    create<Jar>(sourceJarTaskName) {
        group = BasePlugin.BUILD_GROUP

        archiveClassifier.set("sources")
        from(android.sourceSets[SourceSet.MAIN_SOURCE_SET_NAME].java.srcDirs)
    }
    create<Jar>(javadocJarTaskName) {
        group = BasePlugin.BUILD_GROUP

        archiveClassifier.set("javadoc")
        val javadoc = project.tasks[JavaPlugin.JAVADOC_TASK_NAME] as Javadoc
        dependsOn(javadoc)
        from(javadoc.destinationDir)
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["android"])
        groupId = "org.theta4j"
        artifactId = "theta-code-reader"
        version = android.defaultConfig.versionName
        artifact(tasks["sourceJar"])
        artifact(tasks["javadocJar"])
        pom {
            name.set("THETA Code Reader")
            description.set("Client implementation of RICOH THETA API.")
            url.set("https://github.com/theta4j/theta-code-reader")
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

    repositories.maven {
        url = uri("$buildDir/repo")
    }
}

bintray {
    val props = Properties().apply { load(rootProject.file("local.properties").inputStream()) }

    user = props["bintray.user"]?.toString().orEmpty()
    key = props["bintray.key"]?.toString().orEmpty()
    setPublications("maven")
    pkg.apply {
        userOrg = "theta4j"
        repo = "maven"
        name = "theta-code-reader"
        version.apply {
            name = android.defaultConfig.versionName
            vcsTag = "v${android.defaultConfig.versionName}"
            gpg.sign = true
            mavenCentralSync.apply {
                sync = true
                user = props["ossrh.user"]?.toString().orEmpty()
                password = props["ossrh.password"]?.toString().orEmpty()
            }
        }
    }
}

dependencies {
    api("androidx.annotation:annotation:1.1.0")
    implementation("com.google.zxing:core:3.3.3")
    implementation("com.google.zxing:android-core:3.3.0")
}
