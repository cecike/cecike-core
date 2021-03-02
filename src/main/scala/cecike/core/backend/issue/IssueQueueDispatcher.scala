package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.IssueMicroOp
import cecike.utils._

class IssueQueueDispatcherIO extends Bundle {
  val microOpIn = Flipped(DecoupledIO(Vec(decodeWidth, Valid(new IssueMicroOp))))
  val acceptFuTypes = Input(Vec(issueClusterNum, UInt(FunctionUnitType.fuTypeWidth.W)))
  val microOpOut = Vec(issueClusterNum, DecoupledIO(Vec(decodeWidth, Valid(new IssueMicroOp))))
}

class IssueQueueDispatcher extends Module {
  val io = IO(new IssueQueueDispatcherIO)

  assert(!io.acceptFuTypes.reduce(_&_).orR(), "Accepted fu types should be disjoint")

  for (i <- 0 until issueClusterNum) {
    val t = Wire(Vec(decodeWidth, Valid(new IssueMicroOp)))
    t := io.microOpIn.bits
    for (j <- 0 until decodeWidth) {
      t(j).valid := (t(j).bits.fuType & io.acceptFuTypes(i)).orR() &&
        io.microOpIn.bits(j).valid
    }
    io.microOpOut(i).bits := t
  }

  val ready = io.microOpOut.map(_.ready).reduce(_&&_)
  io.microOpIn.ready := ready
  io.microOpOut.foreach(_.valid := ready)
}
