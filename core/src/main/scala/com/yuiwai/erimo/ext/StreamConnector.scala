package com.yuiwai.erimo.ext

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.yuiwai.erimo.Scheduler

trait StreamConnector extends Scheduler {
  protected val source: Source[Payload, NotUsed]
  override def onSchedule(payload: Payload): Unit = {

  }
}
