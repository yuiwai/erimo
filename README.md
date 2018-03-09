# Erimo

Erimoは、Akka Persistenceを利用して状態を永続化するシンプルなスケジューラです。

## Usage

`build.sbt` に依存性を追加します

```
resolvers += "yuiwai repo" at "https://s3-us-west-2.amazonaws.com/repo.yuiwai.com"
libraryDependencies += "com.yuiwai" %% "erimo-core" % "0.2.0"
```

`application.conf` に、 `akka-persistence` の設定を追加します  
インメモリのjournalとローカルファイルのsnapshotを使用する設定例です

```
akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
```

コード例です

即時、1秒後、3秒後の3つのスケジュールを予約し、 `onSchedule` で受け取って、メッセージを出力しています

```scala
import java.time.Instant

import akka.actor.ActorSystem
import com.yuiwai.erimo.Scheduler

import scala.concurrent.duration._

object Example extends App with Scheduler[String] {
  override val schedulerId: String = "test"
  override def onSchedule(payload: String): Unit = {
    println(s"on schedule: payload=$payload")
  }
  override val system = ActorSystem()

  schedule(Instant.now(), "test now")
  schedule(1.second, "test 1sec")
  schedule(3.second, "test 3sec")
}
```

### Stream版

`ScheduleFlow` を利用すると、akka-streamのFlowとしてSchedulerを利用出来ます。

```scala
package com.yuiwai.erimo.example
import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.yuiwai.erimo.ext.SchedulerFlow

object Example extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def instant(sec: Int) = Instant.now().plusSeconds(sec)
  def schedule(sec: Int): (Instant, String) = (instant(sec), s"$sec seconds")
  Source(schedule(3) :: schedule(5) :: schedule(10) :: Nil)
    .via(SchedulerFlow[String]("my-schedule"))
    .runWith(Sink.foreach(println))
}
```

