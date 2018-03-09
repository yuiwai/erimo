package com.yuiwai.erimo.ext

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{KillSwitches, OverflowStrategy, UniqueKillSwitch}
import com.yuiwai.erimo.Scheduler

import scala.reflect.ClassTag

object SchedulerFlow {
  case class Message[Payload](scheduleId: String, payload: Payload)
  def apply[Payload: ClassTag](id: String)(implicit actorSystem: ActorSystem): Flow[(Instant, Payload), Payload, UniqueKillSwitch] = {
    val scheduler = new Scheduler[Payload] {
      override val schedulerId: String = id
      override protected val system: ActorSystem = actorSystem
      override def onSchedule(payload: Payload): Unit = {
        system.eventStream.publish(Message(schedulerId, payload))
      }
    }
    val source = Source.actorRef[Payload](16, OverflowStrategy.dropHead)
      .collect {
        case Message(`id`, payload: Payload) => payload
      }
      .mapMaterializedValue(actorSystem.eventStream.subscribe(_, classOf[Message[Payload]]))
    val sink = Sink.foreach[(Instant, Payload)](s => scheduler.schedule(s._1, s._2))
    Flow.fromSinkAndSource(sink, source).joinMat(KillSwitches.singleBidi[Payload, (Instant, Payload)])(Keep.right)
  }
}

