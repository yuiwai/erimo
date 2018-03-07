package com.yuiwai.erimo.ext

import java.time.Instant

import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.stream._

object SchedulerFlow {
}

class SchedulerFlowGraphStage[Payload](scheduleId: String) extends GraphStage[SourceShape[Payload]] {
  val in: Inlet[(Instant, Payload)] = Inlet("SchedulerFlowGraphStageIn")
  val out: Outlet[Payload] = Outlet("SchedulerFlowGraphStageOut")
  val shape = FlowShape(in, out)
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

  }
}
