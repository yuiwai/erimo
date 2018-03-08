package com.yuiwai.erimo.ext

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, Keep}
import akka.stream.stage._
import com.yuiwai.erimo.Scheduler

import scala.collection.mutable
import scala.concurrent.duration._

object SchedulerFlow {
  def apply[Payload](schedulerId: String)(implicit system: ActorSystem): Flow[(Instant, Payload), Payload, KillSwitch] =
    Flow.fromGraph(new SchedulerFlowGraphStage[Payload](system, schedulerId)).viaMat(KillSwitches.single)(Keep.right)
}

class SchedulerFlowGraphStage[Payload](system: ActorSystem, schedulerId: String) extends GraphStage[FlowShape[(Instant, Payload), Payload]] {
  private val sid = schedulerId
  private val stm = system
  val period = 1.second
  val in: Inlet[(Instant, Payload)] = Inlet[(Instant, Payload)]("SchedulerFlowGraphStage.in")
  val out: Outlet[Payload] = Outlet[Payload]("SchedulerFlowGraphStage.out")
  override def shape: FlowShape[(Instant, Payload), Payload] = FlowShape.of(in, out)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {
    private var downstreamWaiting = false
    private val scheduleQueue: mutable.Queue[Payload] = mutable.Queue.empty
    private val myScheduler = new Scheduler[Payload] {
      override val schedulerId: String = sid
      override protected val system: ActorSystem = stm
      override def onSchedule(payload: Payload): Unit = scheduleQueue.enqueue(payload)
    }
    override def preStart(): Unit = {
      schedulePeriodically(None, period)
    }
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        myScheduler.schedule(elem._1, elem._2)
        pull(in)
      }
    })
    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (scheduleQueue.nonEmpty) {
          push(out, scheduleQueue.dequeue())
          downstreamWaiting = false
        } else {
          downstreamWaiting = true
        }
        if (!hasBeenPulled(in)) pull(in)
      }
    })
    override protected def onTimer(timerKey: Any): Unit = {
      println((downstreamWaiting, scheduleQueue))
      if (scheduleQueue.nonEmpty && downstreamWaiting) {
        push(out, scheduleQueue.dequeue())
        downstreamWaiting = false
      }
    }
  }
}

