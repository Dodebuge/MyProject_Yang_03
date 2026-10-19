plugins {
    kotlin("multiplatform")
}

repositories { mavenCentral() }

kotlin {
    js {
        browser { testTask { enabled = false } }
        nodejs()
        binaries.executable() // Worker.kt의 main()이 워커 진입점
    }
    sourceSets {
        jsTest.dependencies { implementation(kotlin("test")) }
    }
}
