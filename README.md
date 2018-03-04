# Erimo

Erimoは、Akka Persistenceを利用して状態を永続化するシンプルなスケジューラです。

## Usage

`build.sbt` に依存性を追加します

```
resolver += "yuiwai repo" at "https://s3-us-west-2.amazonaws.com/repo.yuiwai.com"
libraryDependencies += "com.yuiwai" %% "erimo-core" % "0.1.0"
```

`application.conf` に、 `akka-persistence` の設定を追加します  
インメモリのjournalとローカルファイルのsnapshotを使用する設定例です

```
akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
```

コード例です

```scala
import java.time.Instant

import akka.actor.ActorSystem
import com.yuiwai.erimo.Scheduler

import scala.concurrent.duration._

object Main extends App with Scheduler {
  override type Payload = String
  override val schedulerId: String = "test"
  override def onSchedule(payload: String): Unit = {
    println(s"on schedule: payload=$payload")
  }
  val system = ActorSystem()

  schedule(Instant.now(), "test now")
  schedule(1.second, "test 1sec")
  schedule(3.second, "test 3sec")
}
```