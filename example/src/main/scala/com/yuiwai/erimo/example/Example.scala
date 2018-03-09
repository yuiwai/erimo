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
