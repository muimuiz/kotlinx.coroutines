<!--[//]: # (title: Coroutines guide)-->
# コルーチン・ガイド

Kotlin は言語として、
他のさまざまなライブラリーがコルーチンを利用できるようにするために、
その標準ライブラリーに最小限の低レベル API しか提供していません。
同様の能力をもった他の多くの言語とは異なって、`async` と `await` は Kotlin のキーワードではなく、
標準ライブラリーの一部にもなっていません。
また、Kotlin の __サスペンド関数__ (suspending function) の概念は、
future や promise よりも安全でエラーの生じにくい非同期操作の抽象化を提供しています。
<!--
Kotlin, as a language, provides only minimal low-level APIs in its standard library to enable various other 
libraries to utilize coroutines. Unlike many other languages with similar capabilities, `async` and `await`
are not keywords in Kotlin and are not even part of its standard library. Moreover, Kotlin's concept
of _suspending function_ provides a safer and less error-prone abstraction for asynchronous 
operations than futures and promises.  
-->

`kotlinx.coroutines` は、JetBrains により開発された充実したコルーチンのライブラリです。
これには `launch` や `async` など、このガイドで取り上げているコルーチンを可能とする高レベルの基本要素が
多数含まれています。
<!--
`kotlinx.coroutines` is a rich library for coroutines developed by JetBrains. It contains a number of high-level 
coroutine-enabled primitives that this guide covers, including `launch`, `async` and others.
-->

これは `kotlinx.coroutines` のコアとなる機能についての一連の例を含み、トピックごとにわけたガイドです。
<!--
This is a guide on core features of `kotlinx.coroutines` with a series of examples, divided up into different topics.
-->

コルーチンを利用するためには、このガイトにある例にならうのと同じく、
[プロジェクトの README](https://github.com/Kotlin/kotlinx.coroutines/blob/master/README.md#using-in-your-projects)
で説明されているように `kotlinx-coroutines-core` モジュールへの依存性を追加する必要があります。
<!--
In order to use coroutines as well as follow the examples in this guide, you need to add a dependency on the `kotlinx-coroutines-core` module as explained 
[in the project README](https://github.com/Kotlin/kotlinx.coroutines/blob/master/README.md#using-in-your-projects).
-->

## もくじ
<!--## Table of contents-->

* [コルーチンの基本](basics.md) ([Coroutines basics](https://kotlinlang.org/docs/coroutines-basics.html))
* [Tutorial: Create a basic coroutine](https://kotlinlang.org/docs/coroutines-basic-jvm.html)
* [Hands-on: Intro to coroutines and channels](https://play.kotlinlang.org/hands-on/Introduction%20to%20Coroutines%20and%20Channels)
* [キャンセルとタイムアウト](cancellation-and-timeouts.md) ([Cancellation and timeouts](https://kotlinlang.org/docs/cancellation-and-timeouts.html))
* [サスペンド関数を構成する](composing-suspending-functions.md) ([Composing suspending functions](https://kotlinlang.org/docs/composing-suspending-functions.html))
* [コルーチンのコンテキストとディスパッチャー](coroutine-context-and-dispatchers.md) ([Coroutine context and dispatchers](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html))
* [非同期の Flow](flow.md)（部分訳） ([Asynchronous Flow](https://kotlinlang.org/docs/flow.html))
* [チャンネル](channels.md) ([Channels](https://kotlinlang.org/docs/channels.html))
* [コルーチンの例外の扱い](exception-handling.md) ([Coroutine exceptions handling](https://kotlinlang.org/docs/exception-handling.html))
* [共有された変更可能な状態と並行性](shared-mutable-state-and-concurrency.html)（部分訳）[Shared mutable state and concurrency](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html)
* [Select expression (experimental)](https://kotlinlang.org/docs/select-expression.html)
* [Tutorial: Debug coroutines using IntelliJ IDEA](https://kotlinlang.org/docs/debug-coroutines-with-idea.html)
* [Tutorial: Debug Kotlin Flow using IntelliJ IDEA](https://kotlinlang.org/docs/debug-flow-with-idea.html)

## 追加の参考資料
<!--## Additional references-->

* [Guide to UI programming with coroutines](../../ui/coroutines-guide-ui.md)
* [Coroutines design document (KEEP)](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md)
* [Full kotlinx.coroutines API reference](https://kotlin.github.io/kotlinx.coroutines)
