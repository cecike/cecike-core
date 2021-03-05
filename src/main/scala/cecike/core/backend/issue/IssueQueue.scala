package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class IssueQueueIO(val fuNum: Int) extends Bundle {
  val microOpIn = DeqIO(Vec(decodeWidth, Valid(new IssueMicroOp)))
  val readyRdMask = Input(UInt(physicalRegisterNum.W))
  val flush = Input(Bool())
  val fuTypes = Input(Vec(fuNum, UInt(FunctionUnitType.fuTypeWidth.W)))
  val busyTable = Input(UInt(physicalRegisterNum.W))

  val acceptFuTypes = Output(UInt(FunctionUnitType.fuTypeWidth.W))
  val microOpOut = Output(Vec(fuNum, DecoupledIO(new IssueMicroOp)))
}

abstract class IssueQueue(fuNum: Int, depth: Int) extends Module {
  require(fuNum > 0)
  require(depth > 0)

  val io = IO(new IssueQueueIO(fuNum))

  io.acceptFuTypes := io.fuTypes.reduce(_|_)
}

abstract class IssueQueueWithCommonEntry(fuNum: Int, depth: Int) extends
  IssueQueue(fuNum, depth) {

  val queueEntries = for (_ <- 0 until depth) yield {
    val entry = Module(new IssueQueueEntry);
    entry
  }
  val queueEntriesIO = VecInit(queueEntries.map(_.io))

  // Common inputs
  queueEntriesIO.foreach { p =>
    p.flush := io.flush
    p.readyRdMask := io.readyRdMask
    p.microOpIn := DontCare
    p.select := false.B
  }
}