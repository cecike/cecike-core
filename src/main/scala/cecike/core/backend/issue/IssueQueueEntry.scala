package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class IssueQueueEntryIO extends Bundle {
  val microOpIn = Input(UndirectionalValid(new IssueMicroOp))
  val readyRdMask = Input(UInt(physicalRegisterNum.W))
  val branchInfo = Input(new BranchInfo)
  val select = Input(Bool())
  val flush = Input(Bool())

  val valid = Output(Bool())
  val ready = Output(Bool())
  val microOpOut = Output(new IssueMicroOp)
}

class IssueQueueEntry extends Module {
  val io = IO(new IssueQueueEntryIO)

  val microOp = Reg(new IssueMicroOp)
  val microOpValid = RegInit(false.B)

  val rs1Match = io.readyRdMask(microOp.rs1Info.addr)
  val rs2Match = io.readyRdMask(microOp.rs2Info.addr)
  val rs1Busy = microOp.rs1Info.busy && (!rs1Match)
  val rs2Busy = microOp.rs2Info.busy && (!rs2Match)
  val ready = microOpValid && (!rs1Busy) && (!rs2Busy)

  when (!io.flush) {
    when (io.microOpIn.valid) {
      microOp := io.microOpIn.bits
      microOpValid := true.B
    }
    when (microOpValid) {
      microOp.rs1Info.busy := rs1Busy
      microOp.rs2Info.busy := rs2Busy
      when (io.select || ((microOp.branchTag & io.branchInfo.tag).orR && io.branchInfo.mispredicted)) {
        microOpValid := false.B
      }
    }
  } otherwise {
    microOpValid := false.B
  }

  io.ready := ready
  io.valid := microOpValid
  io.microOpOut := microOp
}
