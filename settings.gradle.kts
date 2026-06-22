// karoo-ext is published to GitHub Packages, which requires authentication even
// for public packages. Put your GitHub username + a personal access token (with
// the `read:packages` scope) in local.properties as:
//
//   gpr.user=your-github-username
//   gpr.key=ghp_xxxxxxxxxxxxxxxxxxxx
//
// (or export GPR_USER / GPR_KEY environment variables).
fun localProperty(key: String): String? {
    val props = java.util.Properties()
    val file = File(rootDir, "local.properties")
    if (file.isFile) {
        file.inputStream().use { props.load(it) }
    }
    return props.getProperty(key)
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

val env: Map<String, String> = System.getenv()
val gprUser = env["GPR_USER"] ?: localProperty("gpr.user")
val gprKey = env["GPR_KEY"] ?: localProperty("gpr.key")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // karoo-ext SDK
        maven {
            url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
            credentials {
                username = gprUser
                password = gprKey
            }
        }
    }
}

rootProject.name = "Karoo Ride Namer"
include("app")
