<!--- TEST_NAME ComposingGuideTest -->

[//]: # (title: Composing suspending functions)
# サスペンド関数を構成する

この節ではサスペンド関数 (suspending function) を作るさまざまなアプローチを取り扱います。
<!--
This section covers various approaches to composition of suspending functions.
-->

## デフォルトでは逐次的
<!--## Sequential by default-->

別のところで定義され、リモートのサービスコールを行ったり計算を行ったりするような
何かしら有用なことを行う 2 つのサスペンド関数 (suspending function) があるとしましょう。
以下の例のために、それぞれが実際には単に遅延を行うだけのものでそうした有用なものを模倣しています。
<!--
Assume that we have two suspending functions defined elsewhere that do something useful like some kind of 
remote service call or computation. We just pretend they are useful, but actually each one just
delays for a second for the purpose of this example:
-->

```kotlin
suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // ここで何かしら有用なことを行っているふり
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // ここも何かしら有用なことを行っているふり
    return 29
}
```
<!--
suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
-->

もし、はじめに `doSomethingUsefulOne` を、__その後で__ `doSomethingUsefulTwo` を __逐次的__ (sequentially) に呼び出し、それらの結果の和を計算する必要がある場合はどうすればよいでしょう？
実際、最初の関数の結果を使って 2 番目の関数を呼び出すかどうかや、それをどのように呼び出すかについて決める場合、わたしたちはこのようにしています。
<!--
What do we do if we need them to be invoked _sequentially_ &mdash; first `doSomethingUsefulOne` _and then_ 
`doSomethingUsefulTwo`, and compute the sum of their results? 
In practice we do this if we use the result of the first function to make a decision on whether we need 
to invoke the second one or to decide on how to invoke it.
-->

コルーチンのコードは通常のコードとまったく同様にデフォルトでは __逐次的__ なので、通常の逐次的な呼び出しを用います。
以下の例は、両方のサスペンド関数の実行に要した合計時間を測定することによりこのことを確かめています。
<!--
We use a normal sequential invocation, because the code in the coroutine, just like in the regular 
code, is _sequential_ by default. The following example demonstrates it by measuring the total 
time it takes to execute both suspending functions:
-->

<!--- CLEAR -->

```kotlin
    val time = measureTimeMillis {
        val one = doSomethingUsefulOne()
        val two = doSomethingUsefulTwo()
        println("The answer is ${one + two}")
    }
    println("Completed in $time ms")
```
<!--
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
//sampleStart
    val time = measureTimeMillis {
        val one = doSomethingUsefulOne()
        val two = doSomethingUsefulTwo()
        println("The answer is ${one + two}")
    }
    println("Completed in $time ms")
//sampleEnd    
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-01.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-compose-01.kt).-->
<!--{type="note"}-->

これは次のような出力を出します。
<!--
It produces something like this:
-->

```text
The answer is 42
Completed in 2017 ms
```

<!--- TEST ARBITRARY_TIME -->

## async を用いた並行処理
<!--## Concurrent using async-->

`doSomethingUsefulOne` と `doSomethingUsefulTwo` の呼び出しの間に依存性がなく、両者を __並行的__ (concurrently) に実行することでより早く答えを得たいとしたらどうでしょう？
ここで助けとなるのが [async] です。
<!--
What if there are no dependencies between invocations of `doSomethingUsefulOne` and `doSomethingUsefulTwo` and
we want to get the answer faster, by doing both _concurrently_? This is where [async] comes to help. 
-->

概念的には、[async] は [launch] とよく似ています。
これは軽量なスレッドであるところの別のコルーチンを開始し、それは他のコルーチンすべてとは並列に動作します。
違いは `launch` が [Job] を返して結果の値を伝えないのに対し、`async` は [Deferred]、すなわち、あとで結果を与える約束を表している軽量の非ブロッキングな future を返すことです（訳注：future または promise はここでのように並行処理において結果の取得を後回しとする仕組み）。
先延ばしにされた値に対し最終的結果を得るためには、`.await()` を用いることができますが、`Deferred` は `Job` でもあるので、必要ならそれをキャンセルすることもできます。
<!--
Conceptually, [async] is just like [launch]. It starts a separate coroutine which is a light-weight thread 
that works concurrently with all the other coroutines. The difference is that `launch` returns a [Job] and 
does not carry any resulting value, while `async` returns a [Deferred] &mdash; a light-weight non-blocking future
that represents a promise to provide a result later. You can use `.await()` on a deferred value to get its eventual result,
but `Deferred` is also a `Job`, so you can cancel it if needed.
-->

```kotlin
    val time = measureTimeMillis {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
```
<!--
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
//sampleStart
    val time = measureTimeMillis {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
//sampleEnd    
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-02.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-compose-02.kt).-->
<!--{type="note"}-->

これは次のような出力を出します。
<!--
It produces something like this:
-->
```text
The answer is 42
Completed in 1017 ms
```

<!--- TEST ARBITRARY_TIME -->

この 2 つのコルーチンは並行して実行されるので（訳注：実際には delay させているだけのため）2 倍速くなっています。
コルーチンによる並行性は常に明示されることに注意してください。
<!--
This is twice as fast, because the two coroutines execute concurrently. 
Note that concurrency with coroutines is always explicit.
-->

## レイジーに開始される async
<!--## Lazily started async-->

オプションで、[async] の `start` パラメーターに [CoroutineStart.LAZY] を設定することにより [async] をレイジー (lazy) にすることができます（訳注：一般に lazy〔遅延〕はそれが必要となるときまで実際の実行を遅延するときに使われるキーワード）。
このモードでは、コルーチンはその結果が [await][Deferred.await] により要求されたとき、
あるいは `Job` の [start][Job.start] 関数が呼び出されたときになってから開始されます。
以下の例を実行してみましょう。
<!--
Optionally, [async] can be made lazy by setting its `start` parameter to [CoroutineStart.LAZY]. 
In this mode it only starts the coroutine when its result is required by 
[await][Deferred.await], or if its `Job`'s [start][Job.start] function 
is invoked. Run the following example:
-->

```kotlin
    val time = measureTimeMillis {
        val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
        val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
        // なにかの計算
        one.start() // 1 番目のものを起動する
        two.start() // 2 番目のものを起動する
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
```
<!--
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
//sampleStart
    val time = measureTimeMillis {
        val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
        val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
        // some computation
        one.start() // start the first one
        two.start() // start the second one
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
//sampleEnd    
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-03.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-compose-03.kt).-->
<!--{type="note"}-->

これは次のような出力を出します。
<!--
It produces something like this:
-->
```text
The answer is 42
Completed in 1017 ms
```

<!--- TEST ARBITRARY_TIME -->

ここでは 2 つのコルーチンが定義されていますが、前の例とは異なり実行されません。
代わりに、[start][Job.start] を呼んで実際いつ実行を開始するか、制御はプログラマにまかされています。
はじめに `one` を開始し、その後、`two` を開始して、さらにその後でこれら別々のコルーチンが完了するのを待ちます。
<!--
So, here the two coroutines are defined but not executed as in the previous example, but the control is given to
the programmer on when exactly to start the execution by calling [start][Job.start]. We first 
start `one`, then start `two`, and then await for the individual coroutines to finish. 
-->

はじめにそれぞれのコルーチンの [start][Job.start] を呼ぶことなく、
単に `println` の中で [await][Deferred.await] を読んだだけでは、逐次的なふるまいとなることに注意してください。
これは、 [await][Deferred.await] がコルーチンの実行を開始し完了を待つためであり、
レイジーであることに求める有効な事例 (use case) ではありません。
`async(start = CoroutineStart.LAZY)` が有効な場合とは、
値の計算にサスペンド関数が含まれている場合に標準の `lazy` 関数の代替となることです。
<!--
Note that if we just call [await][Deferred.await] in `println` without first calling [start][Job.start] on individual 
coroutines, this will lead to sequential behavior, since [await][Deferred.await] starts the coroutine 
execution and waits for its finish, which is not the intended use-case for laziness. 
The use-case for `async(start = CoroutineStart.LAZY)` is a replacement for the 
standard `lazy` function in cases when computation of the value involves suspending functions.
-->

## async スタイルの関数
<!--## Async-style functions-->

[GlobalScope] 参照を明示した [async] コルーチン・ビルダーを用いて、
`doSomethingUsefulOne` と `doSomethingUsefulTwo` を __非同期__ (asynchronously) に呼び出す async スタイルの関数を定義できます。
こうした関数の名前には "...Async" の接尾をつけて、
それらが非同期計算を始めるだけであり、結果を得るためには結果の遅延値を使う必要があるということを強調することにします。
<!--
We can define async-style functions that invoke `doSomethingUsefulOne` and `doSomethingUsefulTwo`
_asynchronously_ using the [async] coroutine builder with an explicit [GlobalScope] reference.
We name such functions with the 
"...Async" suffix to highlight the fact that they only start asynchronous computation and one needs
to use the resulting deferred value to get the result.
-->

```kotlin
// somethingUsefulOneAsync の返り値は Deferred<Int> です
fun somethingUsefulOneAsync() = GlobalScope.async {
    doSomethingUsefulOne()
}

// somethingUsefulTwoAsync の返り値は Deferred<Int> です
fun somethingUsefulTwoAsync() = GlobalScope.async {
    doSomethingUsefulTwo()
}
```
<!--
// The result type of somethingUsefulOneAsync is Deferred<Int>
fun somethingUsefulOneAsync() = GlobalScope.async {
    doSomethingUsefulOne()
}

// The result type of somethingUsefulTwoAsync is Deferred<Int>
fun somethingUsefulTwoAsync() = GlobalScope.async {
    doSomethingUsefulTwo()
}
-->

`xxxAsync` 関数は __サスペンド__ 関数では「ない」ことに注意しましょう。
これらはどこからでも用いることができます。
しかし、これを用いることは、常に呼び出したコードに対してその動作が非同期的（ここでは __並行的__ の意味）に実行されることを意味します。
<!--
Note that these `xxxAsync` functions are **not** _suspending_ functions. They can be used from anywhere.
However, their use always implies asynchronous (here meaning _concurrent_) execution of their action
with the invoking code.
-->

以下の例は、コルーチン外でのこれらの使用例を示しています。
<!-- 
The following example shows their use outside of coroutine:
-->

<!--- CLEAR -->

```kotlin
// この例では `main` の右に `runBlocking` がない（訳注：メイン・コルーチンを作らない）ことに注意
fun main() {
    val time = measureTimeMillis {
        // コルーチンの外側で async 動作を開始できます。
        val one = somethingUsefulOneAsync()
        val two = somethingUsefulTwoAsync()
        // しかし、結果を待つにはサスペンドするかブロッキングするかどちらかが必要となります。
        // ここでは、結果を待っている間、メイン・スレッドをブロッキングするよう `runBlocking { ... }` を用います
        runBlocking {
            println("The answer is ${one.await() + two.await()}")
        }
    }
    println("Completed in $time ms")
}
```
<!--
import kotlinx.coroutines.*
import kotlin.system.*

//sampleStart
// note that we don't have `runBlocking` to the right of `main` in this example
fun main() {
    val time = measureTimeMillis {
        // we can initiate async actions outside of a coroutine
        val one = somethingUsefulOneAsync()
        val two = somethingUsefulTwoAsync()
        // but waiting for a result must involve either suspending or blocking.
        // here we use `runBlocking { ... }` to block the main thread while waiting for the result
        runBlocking {
            println("The answer is ${one.await() + two.await()}")
        }
    }
    println("Completed in $time ms")
}
//sampleEnd

fun somethingUsefulOneAsync() = GlobalScope.async {
    doSomethingUsefulOne()
}

fun somethingUsefulTwoAsync() = GlobalScope.async {
    doSomethingUsefulTwo()
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-04.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-compose-04.kt).-->
<!--{type="note"}-->

<!--- TEST ARBITRARY_TIME-->
```Text
The answer is 42
Completed in 1085 ms
```
<!-- -->

> ここでの async 関数を用いたプログラミングのスタイルは、
> 他のプログラミング言語では一般的なスタイルであることから、説明のためだけに与えられています。
> Kotlin でこのスタイルを用いることは、以下に説明する理由により「まったくお勧めできません」。
>
<!--
> This programming style with async functions is provided here only for illustration, because it is a popular style
> in other programming languages. Using this style with Kotlin coroutines is **strongly discouraged** for the
> reasons explained below.
>
-->
<!--{type="note"}-->

`val one = somethingUsefulOneAsync()` の行と `one.await()` の式との間で、
コードに何かしらの論理的エラーがあり、プログラムが例外を送出して、
プログラムにより実行中の操作が中断したなら何が起こるか考えてみましょう。
通常なら、グローバルなエラー・ハンドラーがこうした例外を補足して、
開発者のためにエラーを記録し報告する一方で、プログラムはそれ以外の操作を継続できるでしょう。
しかしここでは、それを開始した操作が中断されているにも関わらず、
`somethingUsefulOneAsync` が依然としてバックグラウンドで実行されています。
この問題は、以下の節で示すように構造化された並行性 (structured concurrency) の元では起こりません。
<!--
Consider what happens if between the `val one = somethingUsefulOneAsync()` line and `one.await()` expression there is some logic
error in the code and the program throws an exception and the operation that was being performed by the program aborts. 
Normally, a global error-handler could catch this exception, log and report the error for developers, but the program 
could otherwise continue doing other operations. But here we have `somethingUsefulOneAsync` still running in the background,
even though the operation that initiated it was aborted. This problem does not happen with structured
concurrency, as shown in the section below.
-->

## async をもつ構造化された並行性
<!--## Structured concurrency with async-->

[async を用いた並行処理](#async-を用いた並行処理) を例に取り、
`doSomethingUsefulOne` と `doSomethingUsefulTwo` を同時に実行し、その結果の合計を返す関数を抽出してみましょう。
[async] コルーチン・ビルダーは [CoroutineScope] の拡張として定義されているので、
それをそのスコープ内に持つ必要があります。
このスコープは [coroutineScope][_coroutineScope] 関数が提供します。
<!--
Let us take the [Concurrent using async](#concurrent-using-async) example and extract a function that 
concurrently performs `doSomethingUsefulOne` and `doSomethingUsefulTwo` and returns the sum of their results.
Because the [async] coroutine builder is defined as an extension on [CoroutineScope], we need to have it in the 
scope and that is what the [coroutineScope][_coroutineScope] function provides:
-->

```kotlin
suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    one.await() + two.await()
}
```
<!--
suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    one.await() + two.await()
}
-->

このやり方により、この `concurrentSum` 関数のコード内部で何か問題が起こったとき、そのスコープ内で起動されたすべてのコルーチンがキャンセルされることとなります。
<!--
This way, if something goes wrong inside the code of the `concurrentSum` function and it throws an exception,
all the coroutines that were launched in its scope will be cancelled.
-->

<!--- CLEAR -->

```kotlin
    val time = measureTimeMillis {
        println("The answer is ${concurrentSum()}")
    }
    println("Completed in $time ms")
```
<!--
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
//sampleStart
    val time = measureTimeMillis {
        println("The answer is ${concurrentSum()}")
    }
    println("Completed in $time ms")
//sampleEnd    
}

suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    one.await() + two.await()
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-05.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-compose-05.kt).-->
<!--{type="note"}-->

上の `main` 関数の出力が示すように、両方の操作は依然として同時に実行されます。
<!--
We still have concurrent execution of both operations, as evident from the output of the above `main` function: 
-->

```text
The answer is 42
Completed in 1017 ms
```

<!--- TEST ARBITRARY_TIME -->

キャンセルは常にコルーチンの階層をたぐって伝播します。
<!--
Cancellation is always propagated through coroutines hierarchy:
-->

<!--- CLEAR -->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    try {
        failedConcurrentSum()
    } catch(e: ArithmeticException) {
        println("Computation failed with ArithmeticException")
    }
}

suspend fun failedConcurrentSum(): Int = coroutineScope {
    val one = async<Int> { 
        try {
            delay(Long.MAX_VALUE) // 非常に長い計算を模倣しています
            42
        } finally {
            println("First child was cancelled")
        }
    }
    val two = async<Int> { 
        println("Second child throws an exception")
        throw ArithmeticException()
    }
    one.await() + two.await()
}
```
<!--
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    try {
        failedConcurrentSum()
    } catch(e: ArithmeticException) {
        println("Computation failed with ArithmeticException")
    }
}

suspend fun failedConcurrentSum(): Int = coroutineScope {
    val one = async<Int> { 
        try {
            delay(Long.MAX_VALUE) // Emulates very long computation
            42
        } finally {
            println("First child was cancelled")
        }
    }
    val two = async<Int> { 
        println("Second child throws an exception")
        throw ArithmeticException()
    }
    one.await() + two.await()
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-compose-06.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-compose-06.kt).-->
<!--{type="note"}-->

子の一方（ここでは `two`）が失敗したとき、最初の `async` と待機中の親の両方がキャンセルされることに注目してください。
<!--
Note how both the first `async` and the awaiting parent are cancelled on failure of one of the children
(namely, `two`):
-->
```text
Second child throws an exception
First child was cancelled
Computation failed with ArithmeticException
```

<!--- TEST -->

<!--- MODULE kotlinx-coroutines-core -->
<!--- INDEX kotlinx.coroutines -->

[async]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/async.html
[launch]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html
[Job]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/index.html
[Deferred]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/index.html
[CoroutineStart.LAZY]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-start/-l-a-z-y.html
[Deferred.await]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/await.html
[Job.start]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/start.html
[GlobalScope]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-global-scope/index.html
[CoroutineScope]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/index.html
[_coroutineScope]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/coroutine-scope.html

<!--- END -->
