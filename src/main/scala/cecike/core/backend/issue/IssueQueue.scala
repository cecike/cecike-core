package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class IssueQueueIO(val fuNum: Int) extends Bundle {
  val microOpIn = Flipped(DecoupledIO(Vec(decodeWidth, Valid(new IssueMicroOp))))
  val readyRdMask = Input(UInt(physicalRegisterNum.W))
  val flush = Input(Bool())
  val fuTypes = Input(Vec(fuNum, UInt(FunctionUnitType.fuTypeWidth.W)))

  val microOpOut = Output(Vec(fuNum, DecoupledIO(new IssueMicroOp)))
}

abstract class IssueQueue(fuNum: Int, depth: Int) extends Module {
  require(fuNum > 0)
  require(depth > 0)

  val io = IO(new IssueQueueIO(fuNum))
}
