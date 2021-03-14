package cecike.core.backend.issue

import cecike.CecikeModule
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
  val microOpOut = Vec(fuNum, EnqIO(new IssueMicroOp))
}

abstract class IssueQueue(fuNum: Int, depth: Int) extends CecikeModule {
  require(fuNum > 0)
  require(depth > 0)

  val io = IO(new IssueQueueIO(fuNum))

  io.acceptFuTypes := io.fuTypes.reduce(_|_)
}

abstract class IssueQueueWithCommonEntry(fuNum: Int, depth: Int) extends
  IssueQueue(fuNum, depth) {

  val queueEntries = Reg(Vec(depth, Valid(new IssueMicroOp)))
  when (reset.asBool() || io.flush) {
    queueEntries.foreach(_.valid := false.B)
  }

  val queueRs1Match = Wire(Vec(depth, Bool()))
  val queueRs2Match = Wire(Vec(depth, Bool()))
  val queueRs1Busy = Wire(Vec(depth, Bool()))
  val queueRs2Busy = Wire(Vec(depth, Bool()))
  val queueReady = Wire(Vec(depth, Bool()))

  for(i <- 0 until depth) {
    queueRs1Match(i) := io.readyRdMask(queueEntries(i).bits.rs1Info.addr)
    queueRs2Match(i) := io.readyRdMask(queueEntries(i).bits.rs2Info.addr)

    queueRs1Busy(i) := queueEntries(i).bits.rs1Info.busy && !queueRs1Match(i)
    queueRs2Busy(i) := queueEntries(i).bits.rs2Info.busy && !queueRs2Match(i)

    queueReady(i) := queueEntries(i).valid && !queueRs1Busy(i) && !queueRs2Busy(i)

    when (queueEntries(i).valid) {
      queueEntries(i).bits.rs1Info.busy := queueRs1Busy(i)
      queueEntries(i).bits.rs2Info.busy := queueRs2Busy(i)
    }
  }
}