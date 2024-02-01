import PublishUtils.configurePublishing

plugins {
    `maven-publish`
    signing
}

group = PublishUtils.GROUP_ID
version = PublishUtils.VERSION

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    configurePublishing()
}

