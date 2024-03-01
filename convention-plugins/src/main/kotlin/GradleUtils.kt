import gradle.kotlin.dsl.accessors._b6bea14fb88fd11e46d6fb1ebe601eab.ext
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

object GradleUtils {

    const val DISABLE_ARTIFACT_ID_CHANGE = "DISABLE_ARTIFACT_ID_CHANGE"

    fun Project.getExtraString(name: String): String? = runCatching {
        requireExtraString(name)
    }.getOrNull()

    fun Project.requireExtraString(name: String): String =
        ext[name].toString()

    /**
     * Artifact id corrects to project structure.
     * For example, module library:data:core will be named "data-core"
     */
    fun Project.correctArtifactId(publication: MavenPublication) = with(publication) {
        val disableArtifactIdChange = getExtraString(DISABLE_ARTIFACT_ID_CHANGE)
        if (disableArtifactIdChange != null) return

        val artifactId = this.artifactId
        val name = artifactId.substringBefore('-')
        val correctedPath = path.substringBefore(name)

        val prefix = correctedPath.substringBefore(name)
            .substringAfter("library:")
            .replace(':', '-')

        this.artifactId = buildString {
            append(
                prefix,
                artifactId
            )
        }
    }


}
