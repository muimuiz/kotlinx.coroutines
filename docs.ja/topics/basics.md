<!--- TEST_NAME BasicsGuideTest -->

[//]: # (title: Coroutines basics)

この節ではコルーチン (coroutine) の基本コンセプトについて取り扱います。
<!--This section covers basic coroutine concepts.-->

## 最初のコルーチン
<!--## Your first coroutine-->

次のコードを実行しましょう。
<!--Run the following code:-->

```kotlin
import kotlinx.coroutines.*

fun main() {
    GlobalScope.launch { // 新たなコルーチンをバックグラウンドで起動し続行します
        delay(1000L) // 1 秒間、非ブロッキングの遅延を行います（デフォルトの時間単位はミリ秒です）
        println("World!") // 遅延後に印字します
    }
    println("Hello,") // メインのスレッドは、コルーチンが遅延を行っている間、継続します
    Thread.sleep(2000L) // JVM を生かしたままとする（訳注：プログラムを終了させない）ためメイン・スレッドを 2 秒間停止します
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-01.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-01.kt).-->
<!--{type="note"}-->

次の結果が得られるでしょう。
<!--You will see the following result:-->

```text
Hello,
World!
```

<!--- TEST -->

本質的にはコルーチンは軽量のスレッドです。
コルーチンは、[launch] __コルーチン・ビルダー__ によりある [CoroutineScope] のコンテキストにおいて起動されます。
ここでは、新しいコルーチンを [GlobalScope] において起動しています。
これは、その新たなコルーチンの生存期間がアプリケーション全体の生存期間によってのみ限定されていることを意味しています。
<!--
Essentially, coroutines are light-weight threads.
They are launched with [launch] _coroutine builder_ in a context of some [CoroutineScope].
Here we are launching a new coroutine in the [GlobalScope], meaning that the lifetime of the new
coroutine is limited only by the lifetime of the whole application.  
-->

`GlobalScope.launch { ... }` を `thread { ... }` に、`delay(...)` を `Thread.sleep(...)` に置き換えても
同じ結果を得ることができます。
試してみましょう（`kotlin.concurrent.thread` を import するのを忘れないように）。
<!--
You can achieve the same result by replacing
`GlobalScope.launch { ... }` with `thread { ... }`, and `delay(...)` with `Thread.sleep(...)`. 
Try it (don't forget to import `kotlin.concurrent.thread`).
-->

もしあなたがはじめに `GlobalScope.launch` を `thread` に置き換えたとすると、コンパイラーが次のようなエラーを出すでしょう。
<!--
If you start by replacing `GlobalScope.launch` with `thread`, the compiler produces the following error:
-->

```Plain Text
Error: Kotlin: Suspend functions are only allowed to be called from a coroutine or another suspend function
```

これは [delay] が、スレッドはブロッキング (block) させない一方、コルーチンは __サスペンド__ (suspend) する
特殊な __サスペンド関数__ (suspending function) というものであるためで、コルーチンからしか用いることができないためです。 
<!--
That is because [delay] is a special _suspending function_ that does not block a thread, but _suspends_ the
coroutine, and it can be only used from a coroutine.
-->

## ブロッキングと非ブロッキングの世界を橋渡しする
<!--## Bridging blocking and non-blocking worlds-->

上の最初の例は、__非ブロッキング__ (non-blocking) の `delay(...)` と __ブロッキング__ (blocking) する `Thread.sleep(...)` を
同じコード内で混ぜています。
一方がブロッキングしもう一方がブロッキングしないことを自覚し続けるのは難しい<!--easy to lose track-->ものです。
[runBlocking] コルーチン・ビルダーを用いてブロッキングを明示することにしましょう。
<!--
The first example mixes _non-blocking_ `delay(...)` and _blocking_ `Thread.sleep(...)` in the same code. 
It is easy to lose track of which one is blocking and which one is not. 
Let's be explicit about blocking using the [runBlocking] coroutine builder:
-->

```kotlin
import kotlinx.coroutines.*

fun main() { 
    GlobalScope.launch { // 新たなコルーチンをバックグラウンドで起動し続行します
        delay(1000L)
        println("World!")
    }
    println("Hello,") // メイン・スレッドはここから直ちに続行します
    runBlocking {     // しかし、この式でメイン・スレッドはブロッキングされます
        delay(2000L)  // ... JVM を生かしたままとするため 2 秒間遅延する間は
    } 
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-02.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-02.kt).-->
<!--{type="note"}-->

<!--- TEST-->
```Text
Hello,
World!
```
<!-- -->

出力結果は同一ですが、このコードは非ブロッキングな [delay] のみを用いています。
`runBlocking` を起動するメイン・スレッドは、`runBlocking` 内のコルーチンが完了するまで __ブロッキング__ されます<!--blocks 自動詞的に用いられている？-->。
<!--
The result is the same, but this code uses only non-blocking [delay]. 
The main thread invoking `runBlocking` _blocks_ until the coroutine inside `runBlocking` completes. 
-->

この例は、main 関数の実行を `runBlocking` で包む（ラップする）というもっと慣用的なやり方に書き直すこともできます。
<!--
This example can be also rewritten in a more idiomatic way, using `runBlocking` to wrap 
the execution of the main function:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> { // main コルーチンを開始します
    GlobalScope.launch { // 新たなコルーチンをバックグラウンドで起動し続行します
        delay(1000L)
        println("World!")
    }
    println("Hello,") // main コルーチンはここから直ちに続行します
    delay(2000L)      // JVM を生かしたままとするため 2 秒間遅延します
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-03.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-03.kt).-->
<!--{type="note"}-->

<!--- TEST-->
```Text
Hello,
World!
```
<!-- -->

ここで、 `runBlocking<Unit> { ... }` はトップレベルの main コルーチンを開始させるための装着部品（アダプター）として機能しています。
その返り値を明示的に  `Unit` として指定しました。 
これは Kotlin における適正な `main` 関数が `Unit` の返り値を持たねばならない（訳注：何も返さないのでなければならない）ためです。
<!--
Here `runBlocking<Unit> { ... }` works as an adaptor that is used to start the top-level main coroutine. 
We explicitly specify its `Unit` return type, because a well-formed `main` function in Kotlin has to return `Unit`.
-->

これはサスペンド関数に対するユニット・テストを書く方法でもあります。
<!--
This is also a way to write unit tests for suspending functions:
-->

<!--- INCLUDE
import kotlinx.coroutines.*
-->

```kotlin
class MyTest {
    @Test
    fun testMySuspendingFunction() = runBlocking<Unit> {
        // ここで希望する任意のアサーション（表明）スタイルを用いたサスペンド関数を使うことができます
    }
}
```

<!--- CLEAR -->

## ジョブを待機する
<!--## Waiting for a job-->

別のコルーチンが動いている間、決まった時間の遅延を行うというアプローチはよいものではありません。
起動したバックグラウンドの [Job] が完了するまで明示的に（非ブロッキングな方法で）待機するようにしましょう。
<!--
Delaying for a time while another coroutine is working is not a good approach. Let's explicitly 
wait (in a non-blocking way) until the background [Job] that we have launched is complete:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = GlobalScope.launch { // 新たなコルーチンを起動し、その Job への参照を保持する
        delay(1000L)
        println("World!")
    }
    println("Hello,")
    job.join() // 子のコルーチンが完了するまで待機する
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-04.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-04.kt).-->
<!--{type="note"}-->

<!--- TEST-->
```Text
Hello,
World!
```
<!-- -->

これでも出力結果は変わりませんが、
main コルーチンのコードはバックグラウンドのジョブの持続期間に何ら（訳注：コード上で）拘束されていません。
ずっとよくなりました。
<!--
Now the result is still the same, but the code of the main coroutine is not tied to the duration of
the background job in any way. Much better.
-->

## 構造化並列性
<!--## Structured concurrency-->

実用的にコルーチンを用いるときには、まだ必要とされるものがあります。
`GlobalScope.launch` を用いると、トップレベルのコルーチンが生成されます。
これは負荷が小さいとはいっても、それが実行されている間に依然メモリ資源をいくらか消費します。
新しく起動したコルーチンへの参照を保持しておくことを忘れたとしてもこれはなお実行されます。
もしもこのコルーチンがハングアップしてしまったなら（例えば、間違って非常に長い時間 delay させてしまったなら）、
あるいは多くのコルーチンを起動しすぎてメモリを使い果たしてしまったならどうなるでしょうか？
起動したコルーチンすべてへの参照を手書きで保持しなければならず、またそれらを join しなければならないことは、
エラーの元となります。
<!--
There is still something to be desired for practical usage of coroutines. 
When we use `GlobalScope.launch`, we create a top-level coroutine. Even though it is light-weight, it still 
consumes some memory resources while it runs. If we forget to keep a reference to the newly launched 
coroutine, it still runs. What if the code in the coroutine hangs (for example, we erroneously
delay for too long), what if we launched too many coroutines and ran out of memory? 
Having to manually keep references to all the launched coroutines and [join][Job.join] them is error-prone. 
-->

もっとよい解決策があります。
コードに構造化された並列性 (structured concurrency) を使うことです。
ふつうにスレッドの間で行うのと同じように（スレッドは常にグローバルです）[GlobalScope] においてコルーチンを起動する代わりに、
いま実行している操作の特定のスコープにおいてコルーチンを起動することができます。
（訳注：構造化並列性 \[structured concurrency\] は並列に実行される〔同時実行される〕関数が明確に入れ子になっており、呼ばれた側が終了するまで呼び出した側が終了しないことをいう。）
<!--
There is a better solution. We can use structured concurrency in our code. 
Instead of launching coroutines in the [GlobalScope], just like we usually do with threads (threads are always global), 
we can launch coroutines in the specific scope of the operation we are performing. 
-->

ここでの例にそって言えば、まず [runBlocking] コルーチン・ビルダーを用いてコルーチンへと変更された `main` 関数があります。
`runBlocking` を含むすべてのコルーチン・ビルダーは、[CoroutineScope] のインスタンスをそのコード・ブロックのスコープへと追加します。
外側のコルーチン（ここでの例では `runBlocking`）は、そのスコープ内で起動されたすべてのコルーチンが完了するまで完了しないため、
明示的に `join` することなくこのスコープでコルーチンを起動できます。
よって、ここでの例は次のようにもっと単純なものにできます。
<!--
In our example, we have a `main` function that is turned into a coroutine using the [runBlocking] coroutine builder.
Every coroutine builder, including `runBlocking`, adds an instance of [CoroutineScope] to the scope of its code block. 
We can launch coroutines in this scope without having to `join` them explicitly, because
an outer coroutine (`runBlocking` in our example) does not complete until all the coroutines launched
in its scope complete. Thus, we can make our example simpler:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking { // this: CoroutineScope
    launch { // 新たなコルーチンを runBlocking のスコープ内で起動します
        delay(1000L)
        println("World!")
    }
    println("Hello,")
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-05.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-05.kt).-->
<!--{type="note"}-->

<!--- TEST-->
```Text
Hello,
World!
```
<!-- -->

## スコープ・ビルダー
<!--## Scope builder-->

異なったビルダーによって与えられるコルーチンのスコープに加えて、
[coroutineScope][_coroutineScope] ビルダーを用いれば独自のスコープを宣言することができます。
これは新たなコルーチンのスコープを作成し、起動された子のコルーチンすべてが完了するまで完了しません。
<!--
In addition to the coroutine scope provided by different builders, it is possible to declare your own scope using the
[coroutineScope][_coroutineScope] builder. It creates a coroutine scope and does not complete until all launched children complete. 
-->

[runBlocking] と [coroutineScope][_coroutineScope] は、どちらも本体とすべての子が完了することを待機するので似ているように見えます。
主たる違いは、[runBlocking] メソッドが現在のスレッドを __ブロッキング__ して待機するのに対して、
[coroutineScope][_coroutineScope] はサスペンドするだけで、元となっているスレッドを他の目的のために解放することです。
この違いにより、[runBlocking] は通常の関数であり、[coroutineScope][_coroutineScope] はサスペンド関数となります。
<!--
[runBlocking] and [coroutineScope][_coroutineScope] may look similar because they both wait for their body and all its children to complete.
The main difference is that the [runBlocking] method _blocks_ the current thread for waiting,
while [coroutineScope][_coroutineScope] just suspends, releasing the underlying thread for other usages.
Because of that difference, [runBlocking] is a regular function and [coroutineScope][_coroutineScope] is a suspending function.
-->

このことは次の例に示されています。
<!--
It can be demonstrated by the following example:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking { // this: CoroutineScope
    launch { 
        delay(200L)
        println("Task from runBlocking")
    }
    
    coroutineScope { // コルーチン・スコープを生成します
        launch {
            delay(500L) 
            println("Task from nested launch")
        }
    
        delay(100L)
        println("Task from coroutine scope") // この行は nested launch の前に出力されます
    }
    
    println("Coroutine scope is over") // この行は nested launch が完了するまで出力されません
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-06.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-06.kt).-->
<!--{type="note"}-->

<!--- TEST-->
```Text
Task from coroutine scope
Task from runBlocking
Task from nested launch
Coroutine scope is over
```
<!-- -->

"Task from coroutine scope" のメッセージの直後に（ネストされた launch を待っている間に）、
"Task from runBlocking" が実行され表示されること
⸺[coroutineScope][_coroutineScope] がまだ完了していないとしても⸺
に注目してください。
<!--
Note that right after the "Task from coroutine scope" message (while waiting for nested launch)
 "Task from runBlocking" is executed and printed — even though the [coroutineScope][_coroutineScope] is not completed yet. 
-->

## 関数抽出によるリファクタリング
<!--## Extract function refactoring-->

`launch { ... }` 内部にあるコードのブロックを別の関数へと抽出してみましょう。
このコードに対する「関数抽出」(Extract function) リファクタリング（構造整理）を行う場合、
新たな関数は `suspend` 修飾子を付けた関数となります。
これは __サスペンド関数__ (suspending function) の最初の例です。
サスペンド関数はコルーチンの内部で通常の関数のように用いることができますが、
（ここでの例の `delay` のように）別のコルーチンの実行をサスペンドするために
さらに他のサスペンド関数を用いることができるというのがその新たな機能です。
<!--
Let's extract the block of code inside `launch { ... }` into a separate function. When you 
perform "Extract function" refactoring on this code, you get a new function with the `suspend` modifier.
This is your first _suspending function_. Suspending functions can be used inside coroutines
just like regular functions, but their additional feature is that they can, in turn, 
use other suspending functions (like `delay` in this example) to _suspend_ execution of a coroutine.
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    launch { doWorld() }
    println("Hello,")
}

// ここで最初となる明示されたサスペンド関数
suspend fun doWorld() {
    delay(1000L)
    println("World!")
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-07.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-07.kt).-->
<!--{type="note"}-->

<!--- TEST-->
```Text
Hello,
World!
```
<!-- -->

しかし、抽出した関数が現在のスコープで起動されるコルーチン・ビルダーを持っていた場合はどうなるでしょうか？
その場合、抽出関数の `suspend` 修飾子だけでは十分ではありません
（訳注：コルーチン・ビルダー launch は CoroutineScope のメソッドなので）。
`doWorld` を `CoroutineScope` における拡張メソッドにするというのがひとつの解決法ですが、
API を明快なものとしないので、これはいつでもは適用できないでしょう。
慣用的な解決策は、明示的な `CoroutineScope` を対象となる関数を含むクラスのフィールドとして持つか、
外部のクラスが `CoroutineScope` を実装するときには非明示的に持つかすることです。
最後の手段として、[CoroutineScope(coroutineContext)][CoroutineScope()] を用いることはできますが、
このメソッドの実行のスコープにおける制御をもはや持てないので、
こうしたアプローチは構造的に安全ではありません。
このビルダーはプライベートな API のみで用いることができます。
<!--
But what if the extracted function contains a coroutine builder which is invoked on the current scope?
In this case, the `suspend` modifier on the extracted function is not enough. Making `doWorld` an extension
method on `CoroutineScope` is one of the solutions, but it may not always be applicable as it does not make the API clearer.
The idiomatic solution is to have either an explicit `CoroutineScope` as a field in a class containing the target function
or an implicit one when the outer class implements `CoroutineScope`.
As a last resort, [CoroutineScope(coroutineContext)][CoroutineScope()] can be used, but such an approach is structurally unsafe 
because you no longer have control on the scope of execution of this method. Only private APIs can use this builder.
-->

## コルーチンはとても軽量です
<!--## Coroutines ARE light-weight-->

次のコードを実行しましょう。
<!--Run the following code:-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    repeat(100_000) { // たくさんのコルーチンの起動
        launch {
            delay(5000L)
            print(".")
        }
    }
}
```

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-08.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-08.kt).-->
<!--{type="note"}-->

<!--- TEST lines.size == 1 && lines[0] == ".".repeat(100_000) -->

これは 10 万個のコルーチンを起動し、5 秒後にそれぞれのコルーチンがピリオドを印字します。
<!--
It launches 100K coroutines and, after 5 seconds, each coroutine prints a dot. 
-->

これをスレッドで試してみてください。何が起こるでしょうか？
（たいていは、そのコードは何らかのメモリー不足のエラーを生成するでしょう）
<!--
Now, try that with threads. What would happen? (Most likely your code will produce some sort of out-of-memory error)
-->

## グローバルなコルーチンはデーモン・スレッドに似ています
<!--## Global coroutines are like daemon threads-->

以下のコードは [GlobalScope] で長時間のコルーチンを起動します。
そのコルーチンは 1 秒に 2 度 "I'm sleeping" と印字して、いくらかの遅延の後に main 関数に戻ります。
<!--
The following code launches a long-running coroutine in [GlobalScope] that prints "I'm sleeping" twice a second and then 
returns from the main function after some delay:
-->

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    GlobalScope.launch {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L) // 遅延の後で単に終了する
}
```
<!--{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}-->

> 完全なコードは [ここ](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-09.kt) で入手できます。
>
<!-- > You can get the full code [here](../../kotlinx-coroutines-core/jvm/test/guide/example-basic-09.kt).-->
<!--{type="note"}-->

実行してみると、3 行を印字し終了するのがわかります。
<!--
You can run and see that it prints three lines and terminates:
-->

```text
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
```

<!--- TEST -->

[GlobalScope] において起動されたアクティブなコルーチンは、プロセスを生きたままとしません。
これはデーモン・スレッドに似ています。
<!--
Active coroutines that were launched in [GlobalScope] do not keep the process alive. They are like daemon threads.
-->

<!--- MODULE kotlinx-coroutines-core -->
<!--- INDEX kotlinx.coroutines -->

[launch]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/launch.html
[CoroutineScope]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/index.html
[GlobalScope]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-global-scope/index.html
[delay]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/delay.html
[runBlocking]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html
[Job]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/index.html
[Job.join]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/join.html
[_coroutineScope]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/coroutine-scope.html
[CoroutineScope()]: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope.html

<!--- END -->
