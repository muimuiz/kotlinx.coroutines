<!--- TEST_NAME SharedStateGuideTest -->

# 共有された変更可能な状態と並行性
<!--[//]: # (title: Shared mutable state and concurrency)-->

[Dispatchers.Default] のような複数スレッド（マルチスレッド）を用いるディスパッチャーを用いてコルーチンは並行的に実行でき、
通常の並行処理の問題すべてが引き起こされます。
主たる問題は、**共有された変更可能（ミュータブル）な状態** (shared mutable state) へのアクセスの同期化です。。
コルーチンの世界におけるこの問題への解決策のいくつかは複数スレッドの世界での解決策と同様ですが、ユニークなものもあります。
<!--
Coroutines can be executed concurrently using a multi-threaded dispatcher like the [Dispatchers.Default]. It presents
all the usual concurrency problems. The main problem being synchronization of access to **shared mutable state**. 
Some solutions to this problem in the land of coroutines are similar to the solutions in the multi-threaded world, 
but others are unique.
-->

## 問題
<!--## The problem-->

100 個のコルーチンを起動し全部が同じ作業を 1000 回行ってみましょう。
さらに、後で比較するために完了までの時間も測定します。
<!--
Let us launch a hundred coroutines all doing the same action thousand times. 
We'll also measure their completion time for further comparisons:
-->

```kotlin
suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // 起動するコルーチンの数
    val k = 1000 // 各コルーチンで繰り返す作業の数
    val time = measureTimeMillis {
        coroutineScope { // コルーチンのためのスコープ 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}
```
<!--kotlin
suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}
-->

最初は複数スレッドの [Dispatchers.Default] を用いて、
共有されている変更可能な変数をインクリメントするという非常に単純な作業から始めます。
<!--
We start with a very simple action that increments a shared mutable variable using 
multi-threaded [Dispatchers.Default].
-->

<!--- CLEAR -->

```kotlin
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
```
<!--kotlin
import kotlinx.coroutines.*
import kotlin.system.*    

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
//sampleEnd    
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-01.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-sync-01.kt).-->
<!--{type="note"}-->

<!--- TEST LINES_START
Completed 100000 actions in
Counter =
-->

最後に何を表示するでしょう？
100 個のコルーチンが複数のスレッドから同時になんら同期せずに `counter` をインクリメントするため、
"Counter = 100000" と表示することはまずありません。
<!--
What does it print at the end? It is highly unlikely to ever print "Counter = 100000", because a hundred coroutines increment the `counter` concurrently from multiple threads without any synchronization.
-->

## Volatile は助けとなりません
<!--## Volatiles are of no help-->

変数を `volatile` とすることが並行性の問題を解決するというのはよくある誤解です。
試してみましょう。
<!--
There is a common misconception that making a variable `volatile` solves concurrency problem. Let us try it:
-->

<!--- CLEAR -->

```kotlin
@Volatile // Kotlin の `volatile` はアノテーションです
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
```
<!--kotlin
import kotlinx.coroutines.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
@Volatile // in Kotlin `volatile` is an annotation 
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
//sampleEnd    
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-02.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-sync-02.kt).-->
<!--{type="note"}-->

<!--- TEST LINES_START
Completed 100000 actions in
Counter =
-->

このコードは遅くなりますが、依然として最後に "Counter = 100000" が得られません。
なぜなら、volatile の変数は対応する変数をリニアライザブルなように読み書きすることを保証しますが
（リニアライザブル \[linearizable\] とは「アトミック」〔分割できない操作〕であることを意味する技術用語です）、
よりまとまった作業（上の例ではインクリメント）が分割されないことを保証しないからです。
<!--
This code works slower, but we still don't get "Counter = 100000" at the end, because volatile variables guarantee
linearizable (this is a technical term for "atomic") reads and writes to the corresponding variable, but
do not provide atomicity of larger actions (increment in our case).
-->

## スレッドセーフなデータ構造
<!--## Thread-safe data structures-->

スレッドに対してもコルーチンに対しても機能する一般的な解決策は、
共有された状態で実行される必要のある対応する操作すべてに、
必要な同期を与えるスレッドセーフ（別名、同期的、リニアライザブル、アトミック）なデータ構造を用いることです。
単純なカウンターの場合なら、
アトミックな `incrementAndGet` 操作を持っている `AtomicInteger` クラスを使うことができます。
<!--
The general solution that works both for threads and for coroutines is to use a thread-safe (aka synchronized,
linearizable, or atomic) data structure that provides all the necessary synchronization for the corresponding 
operations that needs to be performed on a shared state. 
In the case of a simple counter we can use `AtomicInteger` class which has atomic `incrementAndGet` operations:
-->

<!--- CLEAR -->

```kotlin
val counter = AtomicInteger()

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.incrementAndGet()
        }
    }
    println("Counter = $counter")
}
```
<!--kotlin
import kotlinx.coroutines.*
import java.util.concurrent.atomic.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
val counter = AtomicInteger()

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.incrementAndGet()
        }
    }
    println("Counter = $counter")
}
//sampleEnd    
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-03.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-sync-03.kt).-->
<!--{type="note"}-->

<!--- TEST ARBITRARY_TIME
Completed 100000 actions in xxx ms
Counter = 100000
-->

この特定の問題に対して、これは最も速い解決策となります。
これは、簡単なカウンターや、コレクション、キュー、その他の標準的なデータ構造と
それらに対する基本的な操作に対して利用できます。
しかし、すぐに利用できるスレッドセーフな実装がない複雑な状態や、複雑な操作へと簡単に拡張できません。
<!--
This is the fastest solution for this particular problem. It works for plain counters, collections, queues and other
standard data structures and basic operations on them. However, it does not easily scale to complex
state or to complex operations that do not have ready-to-use thread-safe implementations. 
-->

## 細粒度スレッド限定
<!--## Thread confinement fine-grained-->

__スレッド限定__ (thread confinement) は、
特定の共有された状態へのアクセスすべてを単一のスレッドに限定するということは、
共有された変更可能な状態の問題へのアプローチです。
これは典型として UI アプリケーションで用いられており、
すべての UI の状態は単一のイベント・ディスパッチ／アプリケーション・スレッドへと限定されています。
単一スレッドのコンテキストを用いることでこれをコルーチンに適用することは簡単です。
<!--
_Thread confinement_ is an approach to the problem of shared mutable state where all access to the particular shared
state is confined to a single thread. It is typically used in UI applications, where all UI state is confined to 
the single event-dispatch/application thread. It is easy to apply with coroutines by using a  
single-threaded context. 
-->

<!--- CLEAR -->

```kotlin
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            // 各インクリメントを単一スレッドのコンテキストへ限定します
            withContext(counterContext) {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
```
<!--kotlin
import kotlinx.coroutines.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            // confine each increment to a single-threaded context
            withContext(counterContext) {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
//sampleEnd      
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-04.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-sync-04.kt).-->
<!--{type="note"}-->

<!--- TEST ARBITRARY_TIME
Completed 100000 actions in xxx ms
Counter = 100000
-->

__細粒度__（fine-grained、小さなループなど）のスレッド限定を行うため、このコードの動作は非常に遅いものです。
各々の独立したインクリメントが、複数スレッドの [Dispatchers.Default] コンテキストを
[withContext(counterContext)][withContext] を用いた単一スレッドのコンテキストへと切り替えています。
<!--
This code works very slowly, because it does _fine-grained_ thread-confinement. Each individual increment switches 
from multi-threaded [Dispatchers.Default] context to the single-threaded context using 
[withContext(counterContext)][withContext] block.
-->

## 疎粒度なスレッド限定
<!--## Thread confinement coarse-grained-->

実用上、スレッド限定は、例えば状態更新のあるビジネス・ロジックの大きな断片のような大きなかたまりで行われます。
以下の例では、このようにしています。
はじめに各コルーチンは単一スレッドのコンテキストで実行されます。
<!--
In practice, thread confinement is performed in large chunks, e.g. big pieces of state-updating business logic
are confined to the single thread. The following example does it like that, running each coroutine in 
the single-threaded context to start with.
-->

<!--- CLEAR -->

```kotlin
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    // すべてを単一スレッドのコンテキストへ限定します
    withContext(counterContext) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
```
<!--kotlin
import kotlinx.coroutines.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    // confine everything to a single-threaded context
    withContext(counterContext) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
//sampleEnd     
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-05.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-sync-05.kt).-->
<!--{type="note"}-->

<!--- TEST ARBITRARY_TIME
Completed 100000 actions in xxx ms
Counter = 100000
-->

今度はずっと高速に動作し、正しい結果を与えます。
<!--
This now works much faster and produces correct result.
-->

## 排他制御
<!--## Mutual exclusion-->

排他制御 (mutual exclusion) による問題の解決は、
並行しては決して実行されない __クリティカル・セクション__（critical section、危険領域）で
共有状態のすべての変更を保護するというものです。
このために、ブロッキングの世界で典型的には `synchronized` や `ReentrantLock` を使うことになるでしょう。
コルーチンにおける代替は [Mutex] と呼ばれます。
コルーチンには、クリティカル・セクションを区切るための
[lock][Mutex.lock] と [unlock][Mutex.unlock] 関数があります。
重要な違いは `Mutex.lock()` がサスペンド関数であることです。
これはスレッドをブロッキングしません。
<!--
Mutual exclusion solution to the problem is to protect all modifications of the shared state with a _critical section_
that is never executed concurrently. In a blocking world you'd typically use `synchronized` or `ReentrantLock` for that.
Coroutine's alternative is called [Mutex]. It has [lock][Mutex.lock] and [unlock][Mutex.unlock] functions to 
delimit a critical section. The key difference is that `Mutex.lock()` is a suspending function. It does not block a thread.
-->

また便利なよう `mutex.lock(); try { ... } finally { mutex.unlock() }` のパターンを表す
[withLock] 拡張関数もあります。
<!--
There is also [withLock] extension function that conveniently represents 
`mutex.lock(); try { ... } finally { mutex.unlock() }` pattern:
-->

<!--- CLEAR -->

```kotlin
val mutex = Mutex()
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            // ロックにより各インクリメントを保護します
            mutex.withLock {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
val mutex = Mutex()
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            // protect each increment with lock
            mutex.withLock {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
//sampleEnd    
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-06.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-sync-06.kt).-->
<!--{type="note"}-->

<!--- TEST ARBITRARY_TIME
Completed 100000 actions in xxx ms
Counter = 100000
-->

この例におけるロックは細粒度のため、コストが掛かります。
しかし、共有された状態を周期的に必ず変更しなければならないものの、
この状態が制限されているような自然なスレッドが存在しないというような状況ではよい選択です。
<!--
The locking in this example is fine-grained, so it pays the price. However, it is a good choice for some situations
where you absolutely must modify some shared state periodically, but there is no natural thread that this state
is confined to.
-->

## アクター

[アクター](https://en.wikipedia.org/wiki/Actor_model) (actor) は、
コルーチンと、そのコルーチンに制約され閉じ込められた状態と、
他のコルーチンと通信するためのチャンネルとを組み合わせて構成された実体です。
単純なアクターは関数として書くことができますが、複雑な状態を持つアクターはクラスが適しています。
<!--
An [actor](https://en.wikipedia.org/wiki/Actor_model) is an entity made up of a combination of a coroutine,
the state that is confined and encapsulated into this coroutine,
and a channel to communicate with other coroutines. A simple actor can be written as a function, 
but an actor with a complex state is better suited for a class. 
-->

簡単に済むよう、やってくるメッセージを受信するためにアクターのメールボックス・チャンネルをそのスコープへと、
また送信チャネルを返されるジョブ・オブジェクトへと結びつける [actor] コルーチン・ビルダーがあり、
これにより、アクターへの単一の参照をそのハンドルとして持ち運ぶことができます。
<!--
There is an [actor] coroutine builder that conveniently combines actor's mailbox channel into its 
scope to receive messages from and combines the send channel into the resulting job object, so that a 
single reference to the actor can be carried around as its handle.
-->

アクターを使う第一歩は、アクターが処理しようとしているメッセージのクラスを定義することです。
この目的には、Kotlin の [sealed classes](https://kotlinlang.org/docs/reference/sealed-classes.html) が適しています。
ここでは、カウンターをインクリメントするための `IncCounter` メッセージと、
値を得るための `GetCounter` メッセージを持った `CounterMsg` sealed class を定義しましょう。
後者はレスポンスを送り返す必要があります。
ここではこのために後で知る（通信する）こととなる単一の値を表現した [CompletableDeferred] 通信プリミティブを用いましょう。
<!--
The first step of using an actor is to define a class of messages that an actor is going to process.
Kotlin's [sealed classes](https://kotlinlang.org/docs/reference/sealed-classes.html) are well suited for that purpose.
We define `CounterMsg` sealed class with `IncCounter` message to increment a counter and `GetCounter` message
to get its value. The later needs to send a response. A [CompletableDeferred] communication
primitive, that represents a single value that will be known (communicated) in the future,
is used here for that purpose.
-->

```kotlin
// counterActor のためのメッセージ型
sealed class CounterMsg
object IncCounter : CounterMsg() // インクリメント・カウンターへの一方向のメッセージ
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // 返信をもつリクエスト
```
<!--kotlin
// Message types for counterActor
sealed class CounterMsg
object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply
-->

次に、[actor] コルチーン・ビルダーを用い、アクターを起動する関数を定義します。
<!--
Then we define a function that launches an actor using an [actor] coroutine builder:
-->

```kotlin
// この関数は新しいアクターを起動します
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0 // アクターの状態
    for (msg in channel) { // やってくるメッセージに渡って繰り返します
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}
```
<!--kotlin
// This function launches a new counter actor
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0 // actor state
    for (msg in channel) { // iterate over incoming messages
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}
-->

メインのコードは簡単です。
<!--
The main code is straightforward:
-->

<!--- CLEAR -->

```kotlin
fun main() = runBlocking<Unit> {
    val counter = counterActor() // アクターを生成します
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.send(IncCounter)
        }
    }
    // アクターからカウンターの値を得るためのメッセージを送ります
    val response = CompletableDeferred<Int>()
    counter.send(GetCounter(response))
    println("Counter = ${response.await()}")
    counter.close() // アクターを終了します
}
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

// Message types for counterActor
sealed class CounterMsg
object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply

// This function launches a new counter actor
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0 // actor state
    for (msg in channel) { // iterate over incoming messages
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}

//sampleStart
fun main() = runBlocking<Unit> {
    val counter = counterActor() // create the actor
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.send(IncCounter)
        }
    }
    // send a message to get a counter value from an actor
    val response = CompletableDeferred<Int>()
    counter.send(GetCounter(response))
    println("Counter = ${response.await()}")
    counter.close() // shutdown the actor
}
//sampleEnd    
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-sync-07.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-sync-07.kt).-->
<!--{type="note"}-->

<!--- TEST ARBITRARY_TIME
Completed 100000 actions in xxx ms
Counter = 100000
-->

アクターそれ自体が実行されるコンテキストは（正確性において）問題ではありません。
アクターはコルーチンであり、ひとつのコルーチンは逐次的に実行されるので、
状態を特定のコルーチンへと制限することが共有された変更可能な状態の問題への解決となります。
実際、アクターはそれ自体のプライベートな状態を変更できますが、
互いにはメッセージを介してしか影響しあえません（どんなロックの必要もありません）。
<!--
It does not matter (for correctness) what context the actor itself is executed in. An actor is
a coroutine and a coroutine is executed sequentially, so confinement of the state to the specific coroutine
works as a solution to the problem of shared mutable state. Indeed, actors may modify their own private state, 
but can only affect each other through messages (avoiding the need for any locks).
-->

アクターは、負荷が大きなときのロックよりも効率的です。
そうした場合、常になすべき作業を行っており、別のコンテキストへと切り替える必要がまったくないからです。
<!--
Actor is more efficient than locking under load, because in this case it always has work to do and it does not 
have to switch to a different context at all.
-->

> [actor] コルーチン・ビルダーは [produce] コルーチン・ビルダーと対となっていることに注意しましょう。
> アクターはメッセージを受信するチャンネルと関連付けられており、
> プロデューサーは要素を送信するチャンネルと関連付けられています。
>
<!--
> Note that an [actor] coroutine builder is a dual of [produce] coroutine builder. An actor is associated 
> with the channel that it receives messages from, while a producer is associated with the channel that it 
> sends elements to.
>
-->
<!--{type="note"}-->

<!--- MODULE kotlinx-coroutines-core -->
<!--- INDEX kotlinx.coroutines -->

[Dispatchers.Default]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-default.html
[withContext]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html
[CompletableDeferred]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-completable-deferred/index.html

<!--- INDEX kotlinx.coroutines.sync -->

[Mutex]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/index.html
[Mutex.lock]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/lock.html
[Mutex.unlock]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/unlock.html
[withLock]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/with-lock.html

<!--- INDEX kotlinx.coroutines.channels -->

[actor]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/actor.html
[produce]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/produce.html

<!--- END -->
