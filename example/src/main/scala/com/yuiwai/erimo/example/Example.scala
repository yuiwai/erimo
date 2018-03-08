package com.yuiwai.erimo.example
import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.yuiwai.erimo.ext.SchedulerFlow

object Example extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val instant = Instant.now().plusSeconds(3)
  val killSwitch = Source.single((instant, "3 seconds"))
    .via(SchedulerFlow[String]("my-schedule"))
    .toMat(Sink.foreach(println))(Keep.right)
    .run()
}
