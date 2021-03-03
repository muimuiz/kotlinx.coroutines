<!--- TEST_NAME ChannelsGuideTest -->

# チャンネル
<!--[//]: # (title: Channels)-->

Deferred の値はコルーチンの間で単一の値を伝える便利な方法を提供します。
Channel（チャンネル）は一連の値の流れ（ストリーム）を伝える方法を提供します。
<!--
Deferred values provide a convenient way to transfer a single value between coroutines.
Channels provide a way to transfer a stream of values.
-->

## チャンネルの基本
<!--## Channel basics-->

[Channel] は概念的には `BlockingQueue` ととてもよく似ています。
重要な違いは、ブロッキングする `put` 操作の代わりにサスペンドする [send][SendChannel.send] を持ち、
ブロッキングする `take` 操作の代わりにサスペンドする [receive][ReceiveChannel.receive] を持っていることです。
<!--
A [Channel] is conceptually very similar to `BlockingQueue`. One key difference is that
instead of a blocking `put` operation it has a suspending [send][SendChannel.send], and instead of 
a blocking `take` operation it has a suspending [receive][ReceiveChannel.receive].
-->

```kotlin
    val channel = Channel<Int>()
    launch {
        // ここでは CPU に大きな負荷のかかる計算や非同期のロジックとなるかもしれませんが、
        // 5 つの平方数を送るだけとしています
        for (x in 1..5) channel.send(x * x)
    }
    // ここでは受け取った 5 つの整数を表示します
    repeat(5) { println(channel.receive()) }
    println("Done!")
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    val channel = Channel<Int>()
    launch {
        // this might be heavy CPU-consuming computation or async logic, we'll just send five squares
        for (x in 1..5) channel.send(x * x)
    }
    // here we print five received integers:
    repeat(5) { println(channel.receive()) }
    println("Done!")
//sampleEnd
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-01.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-01.kt).-->
<!--{type="note"}-->

このコードの出力は以下です。
<!--
The output of this code is:
-->

```text
1
4
9
16
25
Done!
```

<!--- TEST -->

## チャンネルの終了と反復
<!--## Closing and iteration over channels -->

キュー (queue) とは異なり、チャンネルはそれ以上の要素が来ないことを示すためにクローズ (close) することができます。
受信側では、チャンネルからの要素を受け取るのに通常の `for` ループを用いるのが便利です。
<!--
Unlike a queue, a channel can be closed to indicate that no more elements are coming. 
On the receiver side it is convenient to use a regular `for` loop to receive elements 
from the channel. 
-->

概念的には、 [close][SendChannel.close] はチャンネルへと特別なクローズのトークンを送るようなものです。
このクローズ・トークンを受け取るとすぐに反復は停止し、
これによりクローズの前に送信された要素すべてが受信されることを保証します。
<!--
Conceptually, a [close][SendChannel.close] is like sending a special close token to the channel. 
The iteration stops as soon as this close token is received, so there is a guarantee 
that all previously sent elements before the close are received:
-->

```kotlin
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) channel.send(x * x)
        channel.close() // 送信が完了しました
    }
    // 受信した値を `for` ループを用いて（チャンネルがクローズされるまで）表示します
    for (y in channel) println(y)
    println("Done!")
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) channel.send(x * x)
        channel.close() // we're done sending
    }
    // here we print received values using `for` loop (until the channel is closed)
    for (y in channel) println(y)
    println("Done!")
//sampleEnd
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-02.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-02.kt).-->
<!--{type="note"}-->

<!--- TEST -->
```text
1
4
9
16
25
Done!
```
<!-- -->

## チャンネルのプロデューサーを作る
<!--## Building channel producers-->

コルーチンが一連の要素を生み出すこのパターンは極めて一般的なものです。
これは、並行処理のコードでよく見られる
__プロデューサー＝コンシューマー__ （生産者＝消費者、producer-consumer）パターンの一部分です。
こうしたプロデューサーを、チャンネルをパラメーターとして取る関数として抽象化できるでしょう。
しかし、これは結果は関数から返されねばならないという常識には反することになります。
<!--
The pattern where a coroutine is producing a sequence of elements is quite common. 
This is a part of _producer-consumer_ pattern that is often found in concurrent code. 
You could abstract such a producer into a function that takes channel as its parameter, but this goes contrary
to common sense that results must be returned from functions. 
-->

プロデューサー側でこれを簡単に正しく行える [produce] という名の便利なコルーチン・ビルダーがあり、
コンシューマー側には `for` ループに置き換わる拡張関数 [consumeEach] があります。
<!--
There is a convenient coroutine builder named [produce] that makes it easy to do it right on producer side,
and an extension function [consumeEach], that replaces a `for` loop on the consumer side:
-->

```kotlin
    val squares = produceSquares()
    squares.consumeEach { println(it) }
    println("Done!")
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
    for (x in 1..5) send(x * x)
}

fun main() = runBlocking {
//sampleStart
    val squares = produceSquares()
    squares.consumeEach { println(it) }
    println("Done!")
//sampleEnd
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-03.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-03.kt).-->
<!--{type="note"}-->

<!--- TEST -->
```text
1
4
9
16
25
Done!
```
<!-- -->

## パイプライン
<!--## Pipelines-->

パイプライン (pipeline) は――潜在的には無限の――値のストリームを生み出し続けるようなパターンです。
<!--
A pipeline is a pattern where one coroutine is producing, possibly infinite, stream of values:
-->

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++) // 1 から始まり無限に続く整数のストリーム
}
```
<!--kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++) // infinite stream of integers starting from 1
}
-->

別のひとつまたは複数のコルーチンがこのストリームを消費し (consume)、なにかの処理を行い、別の結果を生産します (produce)。
以下の例では、数は単に平方されます。
<!--
And another coroutine or coroutines are consuming that stream, doing some processing, and producing some other results.
In the example below, the numbers are just squared:
-->

```kotlin
fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}
```
<!--kotlin
fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}
-->

メインのコードはこのパイプライン全体を開始し接続します。
<!--
The main code starts and connects the whole pipeline:
-->

<!--- CLEAR -->

```kotlin
    val numbers = produceNumbers() // 1 からそれ以降の整数を生み出します
    val squares = square(numbers) // 整数の平方
    repeat(5) {
        println(squares.receive()) // はじめの 5 つを表示
    }
    println("Done!") // これで終了
    coroutineContext.cancelChildren() // 子のコルーチンをキャンセルする
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    val numbers = produceNumbers() // produces integers from 1 and on
    val squares = square(numbers) // squares integers
    repeat(5) {
        println(squares.receive()) // print first five
    }
    println("Done!") // we are done
    coroutineContext.cancelChildren() // cancel children coroutines
//sampleEnd
}

fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++) // infinite stream of integers starting from 1
}

fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-04.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-04.kt).-->
<!--{type="note"}-->

<!--- TEST -->
```text
1
4
9
16
25
Done!
```
<!-- -->

> コルーチンを生成する関数はすべて [CoroutineScope] の拡張として定義されています。
> これにより、[構造化された並行性](composing-suspending-functions.md#async-を使った構造化された並行性)
> を頼ることができ、アプリケーションにグローバルなコルーチンが残らないことが確かなものとなります。
>
<!--
> All functions that create coroutines are defined as extensions on [CoroutineScope],
> so that we can rely on [structured concurrency](composing-suspending-functions.md#structured-concurrency-with-async) to make
> sure that we don't have lingering global coroutines in our application.
>
-->
<!--{type="note"}-->

## パイプラインを使った素数
<!--## Prime numbers with pipeline-->

コルーチンのパイプラインを使って素数を生み出すという極端な例を考えてみましょう。
数の無限の長さの列から始めます。
<!--
Let's take pipelines to the extreme with an example that generates prime numbers using a pipeline 
of coroutines. We start with an infinite sequence of numbers.
-->

```kotlin
fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
    var x = start
    while (true) send(x++) // はじめから整数の無限のストリーム
}
```
<!--kotlin
fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
    var x = start
    while (true) send(x++) // infinite stream of integers from start
}
-->

続くパイプラインの段階では、入ってくる数をフィルターし、
与えられた素数で割り切れるすべての数を取り除きます。
<!--
The following pipeline stage filters an incoming stream of numbers, removing all the numbers 
that are divisible by the given prime number:
-->

```kotlin
fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}
```
<!--kotlin
fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}
-->

2 から始まる数のストリームから始め、現在のチャンネルから素数を取り出し、
見つかった各素数に対して新しいパイプラインの段階を起動することで、パイプラインを構築していきます。
<!--
Now we build our pipeline by starting a stream of numbers from 2, taking a prime number from the current channel, 
and launching new pipeline stage for each prime number found:
-->
 
```Plain Text
numbersFrom(2) -> filter(2) -> filter(3) -> filter(5) -> filter(7) ... 
```

以下の例では、メイン・スレッドのコンテキストですべてのパイプラインを実行し、最初の 10 個の素数を表示します。
すべてのコルーチンがメインの [runBlocking] コルーチンのスコープで起動されるため、
開始したコルーチンすべてのリストを明示的にとっておく必要はありません。
最初の 10 個の素数を表示した後で、子のコルーチンすべてをキャンセルするために
[cancelChildren][kotlin.coroutines.CoroutineContext.cancelChildren] 拡張関数を使用しています。
<!--
The following example prints the first ten prime numbers, 
running the whole pipeline in the context of the main thread. Since all the coroutines are launched in
the scope of the main [runBlocking] coroutine 
we don't have to keep an explicit list of all the coroutines we have started. 
We use [cancelChildren][kotlin.coroutines.CoroutineContext.cancelChildren] 
extension function to cancel all the children coroutines after we have printed
the first ten prime numbers.
-->

<!--- CLEAR -->

```kotlin
    var cur = numbersFrom(2)
    repeat(10) {
        val prime = cur.receive()
        println(prime)
        cur = filter(cur, prime)
    }
    coroutineContext.cancelChildren() // main を終了させるためすべての子をキャンセルします
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    var cur = numbersFrom(2)
    repeat(10) {
        val prime = cur.receive()
        println(prime)
        cur = filter(cur, prime)
    }
    coroutineContext.cancelChildren() // cancel all children to let main finish
//sampleEnd    
}

fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
    var x = start
    while (true) send(x++) // infinite stream of integers from start
}

fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-05.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-05.kt).-->
<!--{type="note"}-->

このコードの出力は次のようになります。
<!--
The output of this code is:
-->

```text
2
3
5
7
11
13
17
19
23
29
```

<!--- TEST -->

標準ライブラリにある
[`iterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/iterator.html) 
コルーチン・ビルダーを用いても同じパイプラインを構築できることに注意しましょう。
`produce` を `iterator` に、`send` を `yield` に、`receive` を `next` に、
`ReceiveChannel` を `Iterator` に置き換え、コルーチン・スコープを取り除きます。
`runBlocking` も必要ありません。
しかし、上に示したチャンネルを用いたパイプラインの恩恵は、
[Dispatchers.Default] コンテキストで走らせたなら実際には複数の CPU コアを用いることができることにあります。
<!--
Note that you can build the same pipeline using 
[`iterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/iterator.html) 
coroutine builder from the standard library. 
Replace `produce` with `iterator`, `send` with `yield`, `receive` with `next`, 
`ReceiveChannel` with `Iterator`, and get rid of the coroutine scope. You will not need `runBlocking` either.
However, the benefit of a pipeline that uses channels as shown above is that it can actually use 
multiple CPU cores if you run it in [Dispatchers.Default] context.
-->

いずれにしても、これは素数を見つけるための極端に非実用的な方法です。
実際には、パイプラインには（リモートサービスへの非同期呼び出しのような）他のサスペンド呼び出しがあります。
完全に非同期的な `produce` とは違って、`sequence`/`iterator` は任意のサスペンドを許可しないので、
そうしたパイプラインは `sequence`/`iterator` を使って構築することはできません。
<!--
Anyway, this is an extremely impractical way to find prime numbers. In practice, pipelines do involve some
other suspending invocations (like asynchronous calls to remote services) and these pipelines cannot be
built using `sequence`/`iterator`, because they do not allow arbitrary suspension, unlike
`produce`, which is fully asynchronous.
-->
 
## ファン＝アウト
<!--## Fan-out-->

複数のコルーチンが同じチャンネルから受信して、それらで作業を分担するかもしれません。
ここでは定期的に（1 秒に 10 個の）整数を生み出すプロデューサー（生産者）コルーチンから考えてみましょう。
<!--
Multiple coroutines may receive from the same channel, distributing work between themselves.
Let us start with a producer coroutine that is periodically producing integers 
(ten numbers per second):
-->

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1 // 1 から始めます
    while (true) {
        send(x++) // 次を生み出します
        delay(100) // 0.1 秒待ちます
    }
}
```
<!--kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1 // start from 1
    while (true) {
        send(x++) // produce next
        delay(100) // wait 0.1s
    }
}
-->

これから、いくつかのプロセッサー（加工用）コルーチンを作れます。
ここでの例は、その id と受信した数を表示するだけとします。
<!--
Then we can have several processor coroutines. In this example, they just print their id and
received number:
-->

```kotlin
fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }    
}
```
<!--kotlin
fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }    
}
-->

では、5 つのプロセッサーを起動して、およそ 1 秒作業させてみましょう。
どのようになるでしょうか。
<!--
Now let us launch five processors and let them work for almost a second. See what happens:
-->

<!--- CLEAR -->

```kotlin
    val producer = produceNumbers()
    repeat(5) { launchProcessor(it, producer) }
    delay(950)
    producer.cancel() // プロデューサー・コルーチンをキャンセルし、これによりすべて停止します
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking<Unit> {
//sampleStart
    val producer = produceNumbers()
    repeat(5) { launchProcessor(it, producer) }
    delay(950)
    producer.cancel() // cancel producer coroutine and thus kill them all
//sampleEnd
}

fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1 // start from 1
    while (true) {
        send(x++) // produce next
        delay(100) // wait 0.1s
    }
}

fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }    
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-06.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-06.kt).-->
<!--{type="note"}-->

出力は以下のようなものとなるでしょう。ただし、それぞれ特定整数を受け取ったプロセッサー id は異なってきます。
<!--
The output will be similar to the the following one, albeit the processor ids that receive
each specific integer may be different:
-->

```text
Processor #2 received 1
Processor #4 received 2
Processor #0 received 3
Processor #1 received 4
Processor #3 received 5
Processor #2 received 6
Processor #4 received 7
Processor #0 received 8
Processor #1 received 9
Processor #3 received 10
```

<!--- TEST lines.size == 10 && lines.withIndex().all { (i, line) -> line.startsWith("Processor #") && line.endsWith(" received ${i + 1}") } -->

プロデューサー・コルーチンをキャンセルすることでチャンネルが閉じるため、
プロセッサー・コルーチンが作業しているチャンネルを通じた反復処理も最終的に終了することに注意しましょう。
<!--
Note that cancelling a producer coroutine closes its channel, thus eventually terminating iteration
over the channel that processor coroutines are doing.
-->

また、`launchProcessor` コードのファンアウトを行うため、
`for` ループを使ってどのようにチャンネルを明示的に反復処理しているかにも注意してください。
`consumeEach` とは異なって、この `for` ループのパターンは複数のコルーチンから使用しても完全に安全です。
もしこれらプロセッサー・コルーチンのひとつが失敗したとしても、他のものがチャンネルを処理し続けますが、
`consumeEach` を使って書かれたプロセッサーは、正常終了時や異常終了時も、
つねにその元となっているチャンネルを消費します（キャンセルします）。
<!--
Also, pay attention to how we explicitly iterate over channel with `for` loop to perform fan-out in `launchProcessor` code. 
Unlike `consumeEach`, this `for` loop pattern is perfectly safe to use from multiple coroutines. If one of the processor 
coroutines fails, then others would still be processing the channel, while a processor that is written via `consumeEach` 
always consumes (cancels) the underlying channel on its normal or abnormal completion.     
-->

## ファン＝イン
<!--## Fan-in-->

複数のコルーチンが同じチャンネルに送信することもあります。
例えば、文字列のチャンネルがあり、
サスペンド関数 (suspending function) がこのチャンネルに指定された文字列を指定された遅延で繰り返し送信するとしましょう。
<!--
Multiple coroutines may send to the same channel.
For example, let us have a channel of strings, and a suspending function that 
repeatedly sends a specified string to this channel with a specified delay:
-->

```kotlin
suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}
```
<!--kotlin
suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}
-->

ここで、文字列を送信する 2 つのコルーチンを起動したらどうなるか見てみましょう
（この例では、メイン・スレッドのコンテキストでメイン・コルーチンの子としてこれらを起動します）。
<!--
Now, let us see what happens if we launch a couple of coroutines sending strings 
(in this example we launch them in the context of the main thread as main coroutine's children):
-->

<!--- CLEAR -->

```kotlin
    val channel = Channel<String>()
    launch { sendString(channel, "foo", 200L) }
    launch { sendString(channel, "BAR!", 500L) }
    repeat(6) { // 最初の 6 つを受信します
        println(channel.receive())
    }
    coroutineContext.cancelChildren() // メインを終了するためにすべての子をキャンセルします
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    val channel = Channel<String>()
    launch { sendString(channel, "foo", 200L) }
    launch { sendString(channel, "BAR!", 500L) }
    repeat(6) { // receive first six
        println(channel.receive())
    }
    coroutineContext.cancelChildren() // cancel all children to let main finish
//sampleEnd
}

suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-07.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-07.kt).-->
<!--{type="note"}-->

この出力は以下です。
<!--
The output is:
-->

```text
foo
foo
BAR!
foo
foo
BAR!
```

<!--- TEST -->

## バッファーされたチャンネル
<!--## Buffered channels-->

ここまでに見たチャンネルにはバッファー (buffer) がありませんでした。
バッファーのないチャンネルは、送信側と受信側が出会った（ランデヴー \[rendezvous\] した）とき要素を伝送します。
送信側がはじめに起動されたなら受信側が起動されるまで送信側はサスペンドし、
受信側がはじめに起動されたときは送信側が起動されるまで受信側がサスペンドします。
<!--
The channels shown so far had no buffer. Unbuffered channels transfer elements when sender and receiver 
meet each other (aka rendezvous). If send is invoked first, then it is suspended until receive is invoked, 
if receive is invoked first, it is suspended until send is invoked.
-->

[Channel()] ファクトリー関数と [produce] ビルダーはどちらもオプションで
__バッファー・サイズ__ を指定する `capacity`（容量）パラメーターを取ります。
指定された容量を持ちバッファーがいっぱいとなるとブロッキングする `BlockingQueue` と同様に、
バッファーはサスペンドするまでに送信側が複数の要素を送ることを許します。
<!--
Both [Channel()] factory function and [produce] builder take an optional `capacity` parameter to
specify _buffer size_. Buffer allows senders to send multiple elements before suspending, 
similar to the `BlockingQueue` with a specified capacity, which blocks when buffer is full.
-->

以下のコードをふるまいを見てみましょう。
<!--
Take a look at the behavior of the following code:
-->

```kotlin
    val channel = Channel<Int>(4) // バッファーのあるチャンネルを作成します
    val sender = launch { // 送信側コルーチンを起動します
        repeat(10) {
            println("Sending $it") // 送信する前に各要素を表示します
            channel.send(it) // バッファーがいっぱいとなるとサスペンドすることになります
        }
    }
    // 何も受信していません…しばし待ちます…
    delay(1000)
    sender.cancel() // 送信側コルーチンをキャンセルします
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking<Unit> {
//sampleStart
    val channel = Channel<Int>(4) // create buffered channel
    val sender = launch { // launch sender coroutine
        repeat(10) {
            println("Sending $it") // print before sending each element
            channel.send(it) // will suspend when buffer is full
        }
    }
    // don't receive anything... just wait....
    delay(1000)
    sender.cancel() // cancel sender coroutine
//sampleEnd    
}
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-08.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-08.kt).-->
<!--{type="note"}-->

これは、容量 __4__ のバッファー付きチャンネルを用い、__5__ 回の「送信」を表示します。
<!--
It prints "sending" _five_ times using a buffered channel with capacity of _four_:
-->

```text
Sending 0
Sending 1
Sending 2
Sending 3
Sending 4
```

<!--- TEST -->

最初の 4 つの要素はバッファーに追加され、5 つめの要素を送信しようとしたときに送信側がサスペンドします。
<!--
The first four elements are added to the buffer and the sender suspends when trying to send the fifth one.
-->

## チャンネルは公平です
<!--
## Channels are fair
-->

チャンネルの送信と受信操作は、複数のコルーチンからの起動に関して __公平__ です。
チャンネルの送受信操作は、複数のコルーチンからの呼び出しの順序に関して __公平__ に行われます。
これはファーストイン・ファーストアウト（先入れ先出し）の順序で扱われます。
例えば、最初に `receive` を呼び出したコルーチンがその要素を取得します。
以下の例では、2 つのコルーチン "ping" と "pong" が共有された "table" チャンネルから "ball" オブジェクトを受け取っています。
<!--
Send and receive operations to channels are _fair_ with respect to the order of their invocation from 
multiple coroutines. They are served in first-in first-out order, e.g. the first coroutine to invoke `receive` 
gets the element. In the following example two coroutines "ping" and "pong" are 
receiving the "ball" object from the shared "table" channel. 
-->

```kotlin
data class Ball(var hits: Int)

fun main() = runBlocking {
    val table = Channel<Ball>() // 共有の table
    launch { player("ping", table) }
    launch { player("pong", table) }
    table.send(Ball(0)) // ball をサーブします
    delay(1000) // 1 秒遅延します 
    coroutineContext.cancelChildren() // ゲーム・オーバー、キャンセルします
}

suspend fun player(name: String, table: Channel<Ball>) {
    for (ball in table) { // ループ中で ball を受け取ります
        ball.hits++
        println("$name $ball")
        delay(300) // 少し待ちます
        table.send(ball) // ball を打ち返します
    }
}
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

//sampleStart
data class Ball(var hits: Int)

fun main() = runBlocking {
    val table = Channel<Ball>() // a shared table
    launch { player("ping", table) }
    launch { player("pong", table) }
    table.send(Ball(0)) // serve the ball
    delay(1000) // delay 1 second
    coroutineContext.cancelChildren() // game over, cancel them
}

suspend fun player(name: String, table: Channel<Ball>) {
    for (ball in table) { // receive the ball in a loop
        ball.hits++
        println("$name $ball")
        delay(300) // wait a bit
        table.send(ball) // send the ball back
    }
}
//sampleEnd
-->
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-09.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-09.kt).-->
<!--{type="note"}-->

"ping" コルーチンがまず開始するので、ボールを受ける最初のものになります。
"ping" コルーチンがテーブルへボールを返した後で、直ちにボールの受け取りを再度始めても、
"pong" コルーチンがすでにボールを待っているので、ボールは "pong" コルーチンにより受け取られることになります。
<!--
The "ping" coroutine is started first, so it is the first one to receive the ball. Even though "ping"
coroutine immediately starts receiving the ball again after sending it back to the table, the ball gets
received by the "pong" coroutine, because it was already waiting for it:
-->

```text
ping Ball(hits=1)
pong Ball(hits=2)
ping Ball(hits=3)
pong Ball(hits=4)
```

<!--- TEST -->

使用されている executor の性質により、
ときにチャンネルが不公平に見える実行を生成することがあることに注意が必要です。
詳細については [この問題](https://github.com/Kotlin/kotlinx.coroutines/issues/111) を参照してください。
<!--
Note that sometimes channels may produce executions that look unfair due to the nature of the executor
that is being used. See [this issue](https://github.com/Kotlin/kotlinx.coroutines/issues/111) for details.
-->

## ティッカー・チャンネル
<!--## Ticker channels-->

ティッカー・チャンネル (ticker channel) は、チャンネルからの最後のコンシューム（消費）以降、
与えられた遅延時間ごとに `Unit` をプロデュース（生産）する特殊なランデヴー・チャンネルです
（訳注：ticker の原義は時計のようにチクタクと刻むもの）。
単体では役立たないように見えますが、時間を基本とする複雑な [produce] パイプラインやウィンドウ処理、
その他、時間依存の処理を生成するために有用な構成要素です。
ティッカー・チャンネルは、[select] で "on tick" アクションを実行するために使用することができます。
<!--
Ticker channel is a special rendezvous channel that produces `Unit` every time given delay passes since last consumption from this channel.
Though it may seem to be useless standalone, it is a useful building block to create complex time-based [produce] 
pipelines and operators that do windowing and other time-dependent processing.
Ticker channel can be used in [select] to perform "on tick" action.
-->

こうしたチャンネルを生成するためには [ticker] ファクトリー・メソッドを使用し、
これ以上の要素が必要ないときには [ReceiveChannel.cancel] メソッドを使用します。
<!--
To create such channel use a factory method [ticker]. 
To indicate that no further elements are needed use [ReceiveChannel.cancel] method on it.
-->

では、どのようにこれが働くか見てみましょう。
<!--
Now let's see how it works in practice:
-->

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking<Unit> {
    val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0) // ティッカー・チャンネルを生成します
    var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Initial element is available immediately: $nextElement") // 初期の遅延はありません

    nextElement = withTimeoutOrNull(50) { tickerChannel.receive() } // その後の要素はすべて 100 ms の遅延があります
    println("Next element is not ready in 50 ms: $nextElement")

    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
    println("Next element is ready in 100 ms: $nextElement")

    // 大きな消費の遅延を模倣します
    println("Consumer pauses for 150ms")
    delay(150)
    // その次の要素は直ちに利用可能です
    nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Next element is available immediately after large consumer delay: $nextElement")
    // `receive` 呼び出し間の停止が考慮され、次の要素は早く到着することに注意してください
    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() } 
    println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

    tickerChannel.cancel() // これ以上の要素が必要ないことを指示します
}
```
<!--kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking<Unit> {
    val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0) // create ticker channel
    var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Initial element is available immediately: $nextElement") // no initial delay

    nextElement = withTimeoutOrNull(50) { tickerChannel.receive() } // all subsequent elements have 100ms delay
    println("Next element is not ready in 50 ms: $nextElement")

    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
    println("Next element is ready in 100 ms: $nextElement")

    // Emulate large consumption delays
    println("Consumer pauses for 150ms")
    delay(150)
    // Next element is available immediately
    nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Next element is available immediately after large consumer delay: $nextElement")
    // Note that the pause between `receive` calls is taken into account and next element arrives faster
    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() } 
    println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

    tickerChannel.cancel() // indicate that no more elements are needed
}
-->

> 完全なコードは [ここ](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/test/guide/example-channel-09.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-channel-10.kt).-->
<!--{type="note"}-->

これは以下の行を表示します。
<!--
It prints following lines:
-->

```text
Initial element is available immediately: kotlin.Unit
Next element is not ready in 50 ms: null
Next element is ready in 100 ms: kotlin.Unit
Consumer pauses for 150ms
Next element is available immediately after large consumer delay: kotlin.Unit
Next element is ready in 50ms after consumer pause in 150ms: kotlin.Unit
```

<!--- TEST -->

デフォルトでは、[ticker] はコンシューマーが一時停止する可能性に気を配り、
一時停止があれば次にプロデュースする要素の遅延を調整し、
プロデュースする要素が一定の速さを維持しようとすることに注意してください。
<!--
Note that [ticker] is aware of possible consumer pauses and, by default, adjusts next produced element 
delay if a pause occurs, trying to maintain a fixed rate of produced elements.
-->
 
オプションとして、`mode` パラメーターを [TickerMode.FIXED_DELAY] に等しくすれば、
要素間に固定された遅延を維持するよう指定できます。
<!--
Optionally, a `mode` parameter equal to [TickerMode.FIXED_DELAY] can be specified to maintain a fixed
delay between elements.
--> 

<!--- MODULE kotlinx-coroutines-core -->
<!--- INDEX kotlinx.coroutines -->

[CoroutineScope]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/index.html
[runBlocking]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html
[kotlin.coroutines.CoroutineContext.cancelChildren]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/kotlin.coroutines.-coroutine-context/cancel-children.html
[Dispatchers.Default]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-default.html

<!--- INDEX kotlinx.coroutines.channels -->

[Channel]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/index.html
[SendChannel.send]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-send-channel/send.html
[ReceiveChannel.receive]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-receive-channel/receive.html
[SendChannel.close]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-send-channel/close.html
[produce]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/produce.html
[consumeEach]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/consume-each.html
[Channel()]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel.html
[ticker]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/ticker.html
[ReceiveChannel.cancel]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-receive-channel/cancel.html
[TickerMode.FIXED_DELAY]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-ticker-mode/-f-i-x-e-d_-d-e-l-a-y.html

<!--- INDEX kotlinx.coroutines.selects -->

[select]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.selects/select.html

<!--- END -->
