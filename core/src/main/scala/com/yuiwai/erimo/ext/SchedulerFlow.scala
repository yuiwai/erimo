package com.yuiwai.erimo.ext

import java.time.Instant

import akka.stream.scaladsl.{Flow, Source}

object SchedulerFlow {
  def apply[Payload](schedulerId: String) = Flow.fromSinkAndSource(sender(schedulerId), receiver)
  private def sender[Payload](schedulerId: String) = Flow[(Instant, Payload)].to()
  private def receiver =  Source.empty
}

