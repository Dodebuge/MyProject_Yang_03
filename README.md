# 오목 (Kotlin/JS)

Kotlin/JS로 만든 브라우저용 오목 게임입니다. 15×15 바둑판에서 흑이 먼저 두고, 가로·세로·대각선으로 같은 색 돌 5개 이상을 먼저 이으면 승리합니다.
**AI 대전**(사람 = 흑, AI = 백)과 2인 대전을 지원하며, AI는 별도 라이브러리 모듈(`:ai`)로 만들어 **Web Worker**에서 실행합니다.
**기보 저장 · 리플레이**에서는 AI가 잘못 둔 수를 짚어 주고, 원하는 시점부터 다시 이어 둘 수 있습니다.

## 실행

| 목적 | 명령 | IntelliJ 실행 구성 |
|---|---|---|
| 게임 실행 (개발 서버 + 브라우저) | `./gradlew jsBrowserDevelopmentRun` | `게임 실행` |
| 테스트 (게임 + AI) | `./gradlew jsNodeTest` | `테스트` |
| 배포용 빌드 | `./gradlew jsBrowserDistribution` | – |

필요한 것은 JDK 17뿐입니다. Gradle·Node.js·webpack은 Gradle 래퍼와 Kotlin 플러그인이 자동으로 내려받습니다.

배포용 빌드 결과(`build/dist/js/productionExecutable/`)는 **HTTP 서버로 열어야 합니다.** 브라우저가 `file://`에서는 Web Worker 로드를 막기 때문입니다. 예:

```
cd build/dist/js/productionExecutable
python -m http.server 8000      # 또는 npx serve
# → http://localhost:8000
```

IntelliJ에서는 프로젝트 폴더를 열고 Gradle 동기화가 끝나면, 오른쪽 위 드롭다운에서 실행 구성을 골라 ▶를 누르면 됩니다.

## 구조

![구조도](docs/architecture.png)

인터랙티브 버전은 [`docs/architecture.html`](docs/architecture.html)을 내려받아 브라우저로 여세요. 원본 명세는 `docs/architecture.json`이고 [archify](https://github.com/tt-a1i/archify)로 그렸습니다.

```
.
├── build.gradle.kts              # 게임 모듈 (js: browser + nodejs), ai.js를 리소스로 복사
├── settings.gradle.kts           # include("ai")
├── src/
│   ├── jsMain/
│   │   ├── kotlin/
│   │   │   ├── Gomoku.kt         # 승리 판정 (순수 로직)
│   │   │   └── Main.kt           # 상태 · 입력 · 렌더링 · AI 워커 호출
│   │   └── resources/
│   │       └── index.html        # 페이지 (canvas, AI 토글, 다시 시작 버튼)
│   └── jsTest/kotlin/
│       └── GomokuTest.kt         # 승리 판정 테스트
├── ai/                           # AI 라이브러리 모듈 → ai.js (워커 번들)
│   ├── build.gradle.kts
│   └── src/
│       ├── jsMain/kotlin/
│       │   ├── GomokuAi.kt       # AI 라이브러리 (bestMove)
│       │   └── Worker.kt         # Web Worker 어댑터 (메시지 ↔ 라이브러리 호출)
│       └── jsTest/kotlin/
│           └── GomokuAiTest.kt   # AI 테스트
├── .run/                         # IntelliJ 실행 구성 (게임 실행 / 테스트)
├── kotlin-js-store/yarn.lock     # npm 의존성 고정
└── docs/                         # 구조도
```

## 기보 · 리플레이

1. 대국 중이나 대국이 끝난 뒤 **기보 저장**을 누르면 지금까지의 수순이 저장됩니다. 저장 위치는 브라우저 `localStorage`(`gomoku.records` 키)라서 **같은 브라우저에서만** 보입니다.
2. 목록에서 기보를 고르고 **리플레이**를 누르면 마지막 수 화면이 열리고, AI 워커가 모든 수를 분석합니다.
3. **⏮ ◀ ▶ ⏭** 버튼이나 키보드 ← →로 한 수씩 이동합니다.
4. **잘못 둔 수** 목록에는 AI 분석 결과가 `6수 백 F7 — 실수 (추천 C6)`처럼 나옵니다. 항목을 클릭하면 그 수로 이동하고, 판에는 둔 곳이 **빨간 테두리**, AI 추천 수가 **초록 원**으로 표시됩니다.
5. **여기서 다시 시작**을 누르면 보고 있는 시점부터 일반 대국으로 이어서 둡니다. 보고 있는 수가 실수라면 버튼이 **이 수 다시 두기**로 바뀌고, 실수 직전으로 돌아가 그 차례부터 다시 둡니다.

좌표 표기는 가로 A~O(왼쪽부터), 세로 1~15(위부터)입니다.

### 실수 판정

각 수에 대해 그 시점 AI 추천 수의 점수와 실제로 둔 수의 점수 차이(`loss`)를 계산합니다. 점수는 아래 [알고리즘](#알고리즘-패턴-점수)과 같은 패턴 점수입니다.

| 등급 | 기준 | 대표적인 경우 |
|---|---|---|
| 악수 | `loss ≥ 10,000` | 상대의 4를 막지 않음, 이길 수 있는 5목을 놓침 |
| 실수 | `loss ≥ 1,000` | 상대의 열린 3을 방치, 내 열린 3·4 기회를 놓침 |

AI가 한 수 앞만 보기 때문에 판정도 그 수준입니다. 몇 수 뒤에 드러나는 실수는 잡지 못합니다.

## AI

### 구성

```
[메인 스레드: gomoku.js]                       [Web Worker: ai.js]
 Main.kt ── postMessage({id, board, me}) ──▶  Worker.kt ──▶ GomokuAi.bestMove(board, me)
         ◀── postMessage({id, move}) ───────
```

- **`GomokuAi.kt`** (라이브러리): `GomokuAi.bestMove(board: IntArray, me: Int): Int`. DOM이나 워커에 의존하지 않는 순수 함수라서 Node 테스트에서 바로 호출합니다.
- **`Worker.kt`** (어댑터): 메시지를 받아 라이브러리를 호출하고 결과를 돌려보내는 일만 합니다. `ai` 모듈의 `main()`이며, 빌드하면 `ai.js` 번들이 됩니다.
- **게임 쪽** (`Main.kt`): `Worker("ai.js")`로 워커를 띄우고 메시지로만 통신합니다. 게임은 AI 코드에 Gradle 의존성이 없습니다. 루트 `build.gradle.kts`가 `:ai:jsBrowserDistribution` 결과물을 게임 리소스에 복사할 뿐입니다.
- AI 계산이 워커 스레드에서 돌기 때문에 계산 중에도 화면이 멈추지 않습니다.

### 워커 프로토콜

| 방향 | 메시지 | 설명 |
|---|---|---|
| 게임 → AI | `{ id: Number, board: Int32Array \| Number[225], me: 1 \| 2 }` | `board[y * 15 + x]`: 0 빈칸 / 1 흑 / 2 백. `me`: AI가 둘 색 |
| AI → 게임 | `{ id: Number, move: Number }` | `id`는 요청 값 그대로. `move`는 `y * 15 + x`, 둘 곳이 없으면 `-1` |
| 게임 → AI (분석) | `{ id: Number, type: "review", moves: Number[] }` | 흑부터 번갈아 둔 인덱스 목록 |
| AI → 게임 (분석) | `{ id: Number, review: [{ best: Number, loss: Number }, ...] }` | 수마다 그 시점 추천 수(`best`)와 추천 대비 잃은 점수(`loss`, 0이면 최선) |

착수 요청의 `type`은 생략하거나 `"move"`로 보냅니다.

게임은 `id`가 마지막 요청과 다른 응답은 버립니다. "다시 시작"을 누르면 `id`가 바뀌므로 이전 판에 대한 늦은 응답이 새 판에 놓이지 않습니다.

### 다른 언어로 AI 교체하기 (C++ / Python)

위 프로토콜(착수 + 분석)을 지키는 워커 스크립트를 만들고, `Main.kt`의 `Worker("ai.js")` 파일명을 바꾸면 됩니다.

- **C++**: Emscripten으로 `bestMove`를 WASM으로 컴파일하고, 워커 JS에서 `onmessage` → WASM 함수 호출 → `postMessage`.
- **Python**: 워커에서 Pyodide를 로드해 Python `best_move(board, me)`를 호출.

어느 쪽이든 구조는 지금과 같습니다. 순수 라이브러리(`bestMove`)가 있고, 그 앞에 얇은 워커 어댑터가 붙습니다.

### 알고리즘 (패턴 점수)

1. 판이 비어 있으면 천원(가운데, 인덱스 112)에 둡니다.
2. 이미 놓인 돌에서 2칸 이내의 빈칸만 후보로 봅니다.
3. 각 후보에 대해 4방향마다 "여기 두면 몇 개가 이어지고(`count`), 양 끝 중 몇 곳이 비어 있는지(`open`)"를 보고 점수를 매깁니다.

   | 패턴 | 점수 |
   |---|---|
   | 5목 | 100,000 |
   | 열린 4 / 막힌 4 | 10,000 / 1,000 |
   | 열린 3 / 막힌 3 | 1,000 / 100 |
   | 열린 2 / 막힌 2 | 100 / 10 |
   | 양 끝이 모두 막힘 | 0 |

4. `점수 = 공격(내 돌 기준) × 1.1 + 수비(상대 돌 기준)`가 가장 큰 칸에 둡니다. 공격에 가중치를 조금 더 줘서, 이길 수 있는 수가 있으면 상대 4목을 막기보다 이기는 쪽을 고릅니다.

한 수 앞만 보는 휴리스틱이라 띈 삼(`X_XX`)은 알아보지 못합니다. 더 강하게 하려면 이 점수 위에 알파-베타 탐색을 얹으면 됩니다.

## 코드 설명

### `Gomoku.kt` — 규칙

```kotlin
const val N = 15
fun isWin(board: IntArray, x: Int, y: Int): Boolean
```

- 바둑판은 길이 `15 × 15`인 1차원 `IntArray`이고, 칸 `(x, y)`는 `board[y * N + x]`입니다. 값은 `0` 빈칸, `1` 흑, `2` 백입니다.
- `isWin`은 **방금 둔 돌** 하나만 기준으로 검사합니다. 4방향(가로 `1,0`, 세로 `0,1`, 대각 `1,1`, 역대각 `1,-1`) 각각에 대해 양쪽(`+1`, `-1`)으로 같은 색이 이어지는 만큼 세고, 합이 5 이상이면 승리입니다.
- 매 착수마다 판 전체를 훑지 않으므로 한 번의 판정은 최대 4방향 × 14칸입니다.
- `cx in 0 until N` 범위 검사 덕분에 줄 끝에서 다음 줄로 넘어가 이어진 것처럼 세는 일이 없습니다.

### `Main.kt` — 화면, 입력, AI 호출

| 이름 | 역할 |
|---|---|
| `board`, `turn`, `last`, `over` | 게임 상태 (바둑판, 현재 차례 `1`/`2`, 마지막 착수 위치, 종료 여부) |
| `AI = 2` | AI가 두는 색 (백) |
| `ai`, `requestId` | AI 워커와 마지막 요청 번호 |
| `CELL = 36.0` | 교차점 간격(px). 캔버스 크기는 `CELL × (N + 1)` = 576px |
| `pos(i)` | 바둑판 인덱스 → 캔버스 좌표 (`CELL × (i + 1)`, 가장자리 여백 1칸) |
| `aiTurn()` | AI 대전이 켜져 있고, 백 차례이며, 게임이 끝나지 않았는지 |
| `draw()` | 나무색 배경 → 격자선 → 화점 9개 → 돌 → 마지막 수 빨간 점 → 상태 문구 순으로 전체를 다시 그림 |
| `play(i)` | 돌을 놓고 승리 판정 → 차례 변경 → 다시 그리기 → 필요하면 AI에 요청 |
| `askAi()` | AI 차례이면 워커에 `{id, board, me}` 전송 |
| `reset()` | 리플레이 종료, 상태 초기화, `requestId` 증가(늦은 응답 무시), 다시 그리기 |
| `moves` | 기보. 흑부터 번갈아 둔 board 인덱스 목록 (`play()`가 추가) |
| `saveRecord()` / `loadRecords()` | 기보를 `localStorage`에 JSON으로 저장 / 읽기 (저장소를 못 쓰면 빈 목록) |
| `replay`, `step`, `review` | 리플레이 중인 기보, 보고 있는 수 번호, 워커 분석 결과 |
| `startReplay()` | 고른 기보로 리플레이를 열고 워커에 분석(`review`) 요청 |
| `showStep(s)` | 기보의 처음 `s`수만 판에 놓고 다시 그리기 |
| `showReview()` | 분석 결과에서 실수·악수만 골라 클릭 가능한 목록으로 표시 |
| `mistakeLabel(loss)` | `loss`로 "악수" / "실수" / 없음을 판정 |
| `resumeHere()` | 보고 있는 시점(실수면 그 직전)부터 일반 대국으로 전환 |

클릭 처리 흐름:

1. 게임이 끝났거나 AI 차례면 클릭을 무시합니다.
2. 클릭 좌표를 `getBoundingClientRect()` 기준으로 바꾸고, 화면에 맞게 줄어든 캔버스(`max-width: 100%`)를 고려해 `scale`을 곱합니다.
3. `CELL`로 나눈 뒤 반올림해 **가장 가까운 교차점** `(x, y)`를 구합니다. 판 밖이거나 이미 돌이 있으면 무시합니다.
4. `play()`로 돌을 둡니다. 그 결과 AI 차례가 되면 워커에 요청하고, 응답이 오면 다시 `play()`로 AI의 돌을 둡니다.

"AI 대전" 체크박스를 끄면 2인 대전이 되고, 백 차례에 켜면 바로 AI가 둡니다.

### `index.html`

`<canvas id="board">`, 상태 문구 `#status`, AI 대전 체크박스 `#ai`, `#reset`·`#save` 버튼, 저장된 기보 목록 `#records`와 리플레이 패널 `#replay-panel`이 있는 페이지입니다. 컴파일된 `gomoku.js`(파일명은 `settings.gradle.kts`의 `rootProject.name`에서 옴)를 로드하고, `gomoku.js`가 다시 `ai.js`를 워커로 띄웁니다.

### 테스트

- `GomokuTest.kt`: 가로 5목, 대각 5목, 역대각 5목, 4목(승리 아님), 줄 끝에서 다음 줄로 넘어가는 배치(승리 아님).
- `GomokuAiTest.kt`: 첫 수는 천원, 상대 4목 막기, 이길 수 있으면 막기보다 이기기, 꽉 찬 판이면 `-1`, 기보 분석에서 열린 4를 막지 않은 수를 악수로 판정.

### 빌드 설정

- Kotlin Multiplatform 플러그인 2.1.0. 게임과 `ai` 모듈 모두 `js` 타깃에 `browser()`(webpack 번들)와 `nodejs()`(테스트용)를 켰습니다.
- 브라우저 테스트(Karma)는 꺼 두었습니다. 테스트할 브라우저가 설정돼 있지 않으면 아무것도 실행하지 않고 성공으로 표시되기 때문입니다. 테스트는 Node에서만 돌립니다.
- 루트 `jsProcessResources`가 `:ai:jsBrowserDistribution` 결과(`ai.js`)를 게임 리소스에 복사합니다. 그래서 개발 서버와 배포용 빌드 모두 `ai.js`가 `gomoku.js` 옆에 놓입니다.

## 안 넣은 것

렌주룰(흑 3-3·4-4·장목 금수), 무르기, 무승부(판이 꽉 참) 표시, 온라인 대전은 없습니다. 기보는 브라우저 안에만 저장되며 파일 내보내기·불러오기는 없습니다.
