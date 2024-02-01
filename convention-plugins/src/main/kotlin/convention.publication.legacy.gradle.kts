import PublishUtils.configurePublishing

plugins {
    `maven-publish`
    signing
}

apply(from = "${rootDir.absolutePath}/convention-plugins/src/main/kotlin/publish-bundler.gradle")

group = PublishUtils.GROUP_ID
version = PublishUtils.VERSION

artifacts {
    archives(tasks.named("projectSourcesJar"))
    archives(tasks.named("javadocJar"))
}

afterEvaluate {
    configurePublishing {
        artifact(tasks.named("projectSourcesJar"))
    }
}
