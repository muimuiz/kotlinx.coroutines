<!--- TEST_NAME CancellationGuideTest -->

[//]: # (title: Cancellation and timeouts)

この節では、コルーチンのキャンセルとタイムアウトを取り扱います。
<!--This section covers coroutine cancellation and timeouts.-->

## コルーチンの実行をキャンセルする
<!--## Cancelling coroutine execution-->

長時間稼働するアプリケーションにおいては、バックグラウンドのコルーチンを細かく制御する必要があるかもしれません。
例えば、コルーチンを起動したページをユーザーが閉じてしまい、
そのコルーチンの結果はもはや不要で、操作をキャンセルしてもよいようになるかもしれません。
[launch] 関数は、稼働しているコルーチンのキャンセルに用いることのできる [Job] を返します。
<!--
In a long-running application you might need fine-grained control on your background coroutines.
For example, a user might have closed the page that launched a coroutine and now its result
is no longer needed and its operation can be cancelled. 
The [launch] function returns a [Job] that can be used to cancel the running coroutine:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
//sampleStart
    val job = launch {
        repeat(1000) { i ->
            println("job: I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancel() // cancels the job
    job.join() // waits for job's completion 
    println("main: Now I can quit.")
//sampleEnd    
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-01.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-01.kt).-->
<!--{type="note"}-->

これは次のような出力を生成します。
<!--
It produces the following output:
-->

```text
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
main: Now I can quit.
```

<!--- TEST -->

main が `job.cancel` を呼び出すと、他方のコルーチンがキャンセルされるため、それからの出力はその後すぐにもうみられません。
[Job] には、[cancel][Job.cancel] と [join][Job.join] を組み合わせた拡張関数 [cancelAndJoin] もあります。
<!--
As soon as main invokes `job.cancel`, we don't see any output from the other coroutine because it was cancelled. 
There is also a [Job] extension function [cancelAndJoin] 
that combines [cancel][Job.cancel] and [join][Job.join] invocations.
-->

## キャンセルは協調的
<!--## Cancellation is cooperative-->

コルーチンのキャンセルは __協調的__ (cooperative) なものです。
キャンセルが可能であるためにはコルーチンのコードが協調する必要があります。
`kotlinx.coroutines` にあるすべてのサスペンド関数は __キャンセル可能__ (cancellable) です。
それらは、コルーチンのキャンセルをチェックし、キャンセルされたならば [CancellationException] を送出します。
しかし、あるコルーチンが計算を行っていて、キャンセルされたことをチェックしないならば、
以下の例に示すようにそれをキャンセルすることはできません。
<!--
Coroutine cancellation is _cooperative_. A coroutine code has to cooperate to be cancellable.
All the suspending functions in `kotlinx.coroutines` are _cancellable_. They check for cancellation of 
coroutine and throw [CancellationException] when cancelled. However, if a coroutine is working in 
a computation and does not check for cancellation, then it cannot be cancelled, like the following 
example shows:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
//sampleStart
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) { // computation loop, just wastes CPU
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
//sampleEnd    
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-02.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-02.kt).-->
<!--{type="note"}-->

これを実行すると、キャンセルしても
job が 5 回繰り返して自身を完了させるまで "I'm sleeping" を出力し続けることがわかります。
<!--
Run it to see that it continues to print "I'm sleeping" even after cancellation
until the job completes by itself after five iterations.
-->

<!--- TEST-->
```Text
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
job: I'm sleeping 3 ...
job: I'm sleeping 4 ...
main: Now I can quit.
```
<!-- -->

## 計算コードをキャンセル可能にする
<!--## Making computation code cancellable-->

計算コードをキャンセル可能とするには 2 つのアプローチがあります。
第 1 のものは、キャンセルをチェックするサスペンド関数を定期的に呼び出すものです。
この目的に適したものとして [yield] 関数があります。
もうひとつは、キャンセルの状態を明示的にチェックすることです。
この後者のアプローチを試してみましょう。

前の例の `while (i < 5)` を `while (isActive)` に置き換え、再実行してみます。
<!--
There are two approaches to making computation code cancellable. The first one is to periodically 
invoke a suspending function that checks for cancellation. There is a [yield] function that is a good choice for that purpose.
The other one is to explicitly check the cancellation status. Let us try the latter approach. 

Replace `while (i < 5)` in the previous example with `while (isActive)` and rerun it. 
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
//sampleStart
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (isActive) { // cancellable computation loop
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
//sampleEnd    
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-03.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-03.kt).-->
<!--{type="note"}-->

こんどはループがキャンされることがわかります。
[isActive] は、[CoroutineScope] オブジェクトを介してコルーチン内部で利用可能な拡張プロパティです。
<!--
As you can see, now this loop is cancelled. [isActive] is an extension property 
available inside the coroutine via the [CoroutineScope] object.
-->

<!--- TEST-->
```Text
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
main: Now I can quit.
```
<!-- -->

## リソースを `finally` で終了する
<!--## Closing resources with `finally`-->

キャンセル可能なサスペンド関数は、キャンセルのとき通常の方法で扱うことができる [CancellationException] を送出します。
例えば、`try {...} finally {...}` 式と Kotlin の `use` 関数は、
コルーチンがキャンセルされたときその終了処理を通常のように実行します。
<!--
Cancellable suspending functions throw [CancellationException] on cancellation which can be handled in 
the usual way. For example, `try {...} finally {...}` expression and Kotlin `use` function execute their
finalization actions normally when a coroutine is cancelled:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
//sampleStart
    val job = launch {
        try {
            repeat(1000) { i ->
                println("job: I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            println("job: I'm running finally")
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
//sampleEnd    
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-04.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-04.kt).-->
<!--{type="note"}-->

[join][Job.join] と [cancelAndJoin] とはともにすべての終了処理が完了するのを待機するので、
上の例は以下の出力を与えます。
<!--
Both [join][Job.join] and [cancelAndJoin] wait for all finalization actions to complete, 
so the example above produces the following output:
-->

```text
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
job: I'm running finally
main: Now I can quit.
```

<!--- TEST -->

## キャンセル可能でないブロックを実行する
<!--## Run non-cancellable block-->

上の例での `finally` ブロックにおいてサスペンド関数を使おうとすれば、
このコードを実行しているコルーチンがキャンセルされているために常に [CancellationException] を発生させます。
通常、これは問題とはなりません。なぜなら、すべての行儀がよい終了操作（ファイルを閉じることや、ジョブをキャンセルすること、任意の種類の通信チャネルを閉じること）は、ふつう非ブロッキングであり、いかなるサスペンド関数も関与していないからです。
しかし、キャンセルされたコルーチンにおいてサスペンドする必要があるまれな場合には、
対応するコードを [withContext] 関数と [NonCancellable] コンテキストを用いて、
以下の例に示すように `withContext(NonCancellable) {...}` で包むことができます。
<!--
Any attempt to use a suspending function in the `finally` block of the previous example causes
[CancellationException], because the coroutine running this code is cancelled. Usually, this is not a 
problem, since all well-behaving closing operations (closing a file, cancelling a job, or closing any kind of a 
communication channel) are usually non-blocking and do not involve any suspending functions. However, in the 
rare case when you need to suspend in a cancelled coroutine you can wrap the corresponding code in
`withContext(NonCancellable) {...}` using [withContext] function and [NonCancellable] context as the following example shows:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
//sampleStart
    val job = launch {
        try {
            repeat(1000) { i ->
                println("job: I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            withContext(NonCancellable) {
                println("job: I'm running finally")
                delay(1000L)
                println("job: And I've just delayed for 1 sec because I'm non-cancellable")
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
//sampleEnd    
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-05.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-05.kt).-->
<!--{type="note"}-->

<!--- TEST-->
```Text
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
job: I'm running finally
job: And I've just delayed for 1 sec because I'm non-cancellable
main: Now I can quit.
```
<!-- -->

## タイムアウト
<!--## Timeout-->

コルーチンの実行をキャンセルする理由で最も明らかで実用的なものは、
その実行時間があるタイムアウト（時間切れ）の時間を超えたからというものです。
対応する [Job] への参照を手動で追跡し、
間延びした追跡コルーチンをキャンセルするために別のコルーチンを起動することもできますが、
これを行う [withTimeout] 関数が使えます。
次の例を見てみましょう。
<!--
The most obvious practical reason to cancel execution of a coroutine 
is because its execution time has exceeded some timeout.
While you can manually track the reference to the corresponding [Job] and launch a separate coroutine to cancel 
the tracked one after delay, there is a ready to use [withTimeout] function that does it.
Look at the following example:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
//sampleStart
    withTimeout(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
//sampleEnd
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-06.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-06.kt).-->
<!--{type="note"}-->

これは次のような出力を与えます。
<!--
It produces the following output:
-->

```text
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
Exception in thread "main" kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1300 ms
```

<!--- TEST STARTS_WITH -->

[withTimeout] により送出される `TimeoutCancellationException` は [CancellationException] のサブクラスです。
これまでそのスタックトレースがコンソール上に表示されるのを見てきませんでした。
それは、キャンセルされたコルーチン内部においては、
`CancellationException` がコルーチンが終了する通常の理由であるとみなされていたからです。
しかしこの例においては、`withTimeout` を `main` 関数のすぐ内側で使用しています。
<!--
The `TimeoutCancellationException` that is thrown by [withTimeout] is a subclass of [CancellationException].
We have not seen its stack trace printed on the console before. That is because
inside a cancelled coroutine `CancellationException` is considered to be a normal reason for coroutine completion. 
However, in this example we have used `withTimeout` right inside the `main` function. 
-->

キャンセルは単なる例外なので、すべてのリソースは通常の方法で終了されます。
何らかの種類のタイムアウトに対してもし特に何か追加の処理が必要なら、
`try {...} catch (e: TimeoutCancellationException) {...}` ブロックのタイムアウト時のコードをラップできます。
また次のように、[withTimeout] に類似しているものの例外の送出の代わりにタイムアウト時に `null` を返す
[withTimeoutOrNull] 関数を使うこともできます。
<!--
Since cancellation is just an exception, all resources are closed in the usual way. 
You can wrap the code with timeout in a `try {...} catch (e: TimeoutCancellationException) {...}` block if 
you need to do some additional action specifically on any kind of timeout or use the [withTimeoutOrNull] function
that is similar to [withTimeout] but returns `null` on timeout instead of throwing an exception:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
//sampleStart
    val result = withTimeoutOrNull(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
        "Done" // will get cancelled before it produces this result
    }
    println("Result is $result")
//sampleEnd
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-07.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-07.kt).-->
<!--{type="note"}-->

次のように、このコードを実行した場合もはや例外は送出されません。
<!--
There is no longer an exception when running this code:
-->

```text
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
Result is null
```

<!--- TEST -->

## 非同期のタイムアウトとリソース
<!--## Asynchronous timeout and resources-->

<!-- 
  NOTE: Don't change this section name. It is being referenced to from within KDoc of withTimeout functions.
-->

[withTimeout] におけるタイムアウトの発生は、
ブロック内で実行されているコードに関して非同期的 (asynchronous) で、
いつでも、たとえタイムアウトのブロックの内部から戻る直前であったとしても起こる可能性があります。
ブロックの内側で何らかのリソースを開いたり取得したりしてるならば、
そのブロックの外側で閉じたり開放したりする必要があることを覚えておきましょう。
<!--
The timeout event in [withTimeout] is asynchronous with respect to the code running in its block and may happen at any time,
even right before the return from inside of the timeout block. Keep this in mind if you open or acquire some
resource inside the block that needs closing or release outside of the block. 
-->

例えば、ここでは `Resource` クラスを用いてクローズが必要なリソースを模倣しています。
これは `acquired` カウンターをインクリメントするとともに `close` 関数によってこのカウンターをデクリメントすることによって
何回生成されたかを単純に記録します。
小さなタイムアウト時間をもつ多数のコルーチンを実行し、
わずかな遅延の後に `withTimeout` ブロックの内部からこのリソースの取得を試みて、外部でそれを解放してみましょう。
<!--
For example, here we imitate a closeable resource with the `Resource` class, that simply keeps track of how many times 
it was created by incrementing the `acquired` counter and decrementing this counter from its `close` function.
Let us run a lot of coroutines with the small timeout try acquire this resource from inside
of the `withTimeout` block after a bit of delay and release it from outside.
-->

```kotlin
import kotlinx.coroutines.*

//sampleStart
var acquired = 0

class Resource {
    init { acquired++ } // Acquire the resource
    fun close() { acquired-- } // Release the resource
}

fun main() {
    runBlocking {
        repeat(100_000) { // Launch 100K coroutines
            launch { 
                val resource = withTimeout(60) { // Timeout of 60 ms
                    delay(50) // Delay for 50 ms
                    Resource() // Acquire a resource and return it from withTimeout block     
                }
                resource.close() // Release the resource
            }
        }
    }
    // Outside of runBlocking all coroutines have completed
    println(acquired) // Print the number of resources still acquired
}
//sampleEnd
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-08.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-08.kt).-->
<!--{type="note"}-->

<!--- CLEAR -->

上のコードを実行すると、これは常に 0 を出力するわけではないことがわかるでしょう。
マシンのタイミングによりますが、この例で実際に 0 でない値を得るにはタイムアウトの時間を微調整する必要があるかもしれません。
<!--
If you run the above code you'll see that it does not always print zero, though it may depend on the timings 
of your machine you may need to tweak timeouts in this example to actually see non-zero values. 
-->

> 注目すべきことは、ここでの 10 万個のコルーチンによる `acquired` カウンターのインクリメントとデクリメントは、
> 常に同じメインスレッドから行われているので完全に安全であるということです。
> このことについてより詳しいことは、コルーチンのコンテキストに関する次の章において説明します。
> 
<!--
> Note, that incrementing and decrementing `acquired` counter here from 100K coroutines is completely safe,
> since it always happens from the same main thread. More on that will be explained in the next chapter
> on coroutine context.
> 
-->
<!--{type="note"}-->

ここでの問題を回避するために、`withTimeout` ブロックからリソースへの参照を返すのではなく、
それを変数に保持することができます。
<!--
To workaround this problem you can store a reference to the resource in the variable as opposed to returning it 
from the `withTimeout` block. 
-->

```kotlin
import kotlinx.coroutines.*

var acquired = 0

class Resource {
    init { acquired++ } // Acquire the resource
    fun close() { acquired-- } // Release the resource
}

fun main() {
//sampleStart
    runBlocking {
        repeat(100_000) { // Launch 100K coroutines
            launch { 
                var resource: Resource? = null // Not acquired yet
                try {
                    withTimeout(60) { // Timeout of 60 ms
                        delay(50) // Delay for 50 ms
                        resource = Resource() // Store a resource to the variable if acquired      
                    }
                    // We can do something else with the resource here
                } finally {  
                    resource?.close() // Release the resource if it was acquired
                }
            }
        }
    }
    // Outside of runBlocking all coroutines have completed
    println(acquired) // Print the number of resources still acquired
//sampleEnd
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-09.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-cancel-09.kt).-->
<!--{type="note"}-->

This example always prints zero. Resources do not leak.

<!--- TEST 
0
-->

<!--- MODULE kotlinx-coroutines-core -->
<!--- INDEX kotlinx.coroutines -->

[launch]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html
[Job]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/index.html
[cancelAndJoin]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/cancel-and-join.html
[Job.cancel]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/cancel.html
[Job.join]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/join.html
[CancellationException]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-cancellation-exception/index.html
[yield]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/yield.html
[isActive]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/is-active.html
[CoroutineScope]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/index.html
[withContext]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-context.html
[NonCancellable]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-non-cancellable.html
[withTimeout]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-timeout.html
[withTimeoutOrNull]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/with-timeout-or-null.html

<!--- END -->
