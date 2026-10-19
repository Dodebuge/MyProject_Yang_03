plugins {
    kotlin("multiplatform") version "2.1.0"
}

repositories { mavenCentral() }

kotlin {
    js {
        browser { testTask { enabled = false } } // 테스트는 Node로만 실행 (브라우저 미설정 시 조용히 건너뜀)
        nodejs()
        binaries.executable()
    }
    sourceSets {
        jsTest.dependencies { implementation(kotlin("test")) }
    }
}
