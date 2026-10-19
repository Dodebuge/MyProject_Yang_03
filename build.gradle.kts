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

// AI 워커 번들(ai.js)을 게임 리소스 옆에 복사 → new Worker("ai.js")로 로드
tasks.named<Copy>("jsProcessResources") {
    from(project(":ai").tasks.named("jsBrowserDistribution"))
}
