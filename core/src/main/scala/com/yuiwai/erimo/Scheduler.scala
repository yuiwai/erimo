package com.yuiwai.erimo

import java.time.Instant

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}

import scala.collection.SortedSet
import scala.concurrent.duration.Duration

trait Scheduler {
  import SchedulerActor._
  type Payload
  val schedulerId: String
  protected val system: ActorSystem
  private lazy val scheduleActor: ActorRef = system.actorOf(SchedulerActor.props(schedulerId, onSchedule))
  def schedule(at: Instant, payload: Payload): Unit = scheduleActor ! ScheduleCommand(at, payload)
  def schedule(duration: Duration, payload: Payload): Unit =
    schedule(Instant.now().plusSeconds(duration.toSeconds), payload)
  def onSchedule(payload: Payload): Unit
}

private[erimo] class SchedulerActor[P](schedulerId: String, callback: P => Unit) extends PersistentActor {
  import SchedulerActor._
  implicit val scheduleOrdering = new Ordering[Schedule] {
    override def compare(x: Schedule, y: Schedule): Int = {
      x.at.getEpochSecond.compare(y.at.getEpochSecond)
    }
  }
  case class Schedule(at: Instant, payload: P)
  case class State(schedules: SortedSet[Schedule]) {
    def firedSchedules(instant: Instant): SortedSet[Schedule] =
      schedules.takeWhile(_.at.getEpochSecond <= instant.getEpochSecond)
    def updated(event: Event): State = event match {
      case Scheduled(at, payload: P) => copy(schedules + Schedule(at, payload))
      case Fired(at, payload: P) => copy(schedules - Schedule(at, payload))
    }
  }
  private var state: State = State(SortedSet.empty[Schedule])
  override def persistenceId: String = schedulerId
  override def preStart(): Unit = {
    import context.dispatcher

    import scala.concurrent.duration._
    super.preStart()
    context.system.scheduler.schedule(0.second, 1.second, self, TickCommand)
  }
  private def updateState(event: Event) = state = state.updated(event)
  override def receiveRecover: Receive = {
    case event: Event => updateState(event)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }
  override def receiveCommand: Receive = {
    case TickCommand =>
      persistAll[Fired[P]](state.firedSchedules(Instant.now()).map { schedule =>
        Fired(schedule.at, schedule.payload)
      }.toList) { fired =>
        updateState(fired)
        callback(fired.payload)
      }
    case ScheduleCommand(at, payload) =>
      persist(Scheduled(at, payload)) { scheduled =>
        updateState(scheduled)
      }
  }
}

private[erimo] object SchedulerActor {
  def props[P](schedulerId: String, callback: P => Unit): Props =
    Props(new SchedulerActor(schedulerId, callback))
  sealed trait Command
  case object TickCommand extends Command
  final case class ScheduleCommand[P](at: Instant, payload: P) extends Command
  sealed trait Event
  case class Scheduled[P](at: Instant, payload: P) extends Event
  case class Fired[P](at: Instant, payload: P) extends Event
}
