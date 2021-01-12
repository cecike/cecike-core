package cecike.core.common

import chisel3._
import cecike.core.common.Constants._
import cecike.utils._

class SourceRegisterInfo extends Bundle {
  val busy = Bool()
  val addr = UInt(physicalRegisterAddressWidth.W)
}

class IssueMicroOp extends Bundle {
  // Micro Op Info
  val pc = UInt(xLen.W)

  val branchTag = UInt(branchTagWidth.W)
  val branchPredictionResult = new BranchPredictionResult

  val rs1Info = new SourceRegisterInfo
  val rs2Info = new SourceRegisterInfo
  val immediate = UInt(xLen.W)

  val rdInfo = UndirectionedValid(UInt(physicalRegisterAddressWidth.W))
}

object IssueMicroOp {
  def apply(microOpIn: MicroOp) = {
    val microOp = Wire(new IssueMicroOp)
    microOp.pc := microOpIn.pc
    microOp.branchTag := microOpIn.branchTag
    microOp.branchPredictionResult := microOpIn.branchPredictionResult
    microOp.rs1Info.addr := microOpIn.physicalRs1
    microOp.rs1Info.busy := microOpIn.physicalRs1Busy
    microOp.rs2Info.addr := microOpIn.physicalRs2
    microOp.rs2Info.busy := microOpIn.physicalRs2Busy
    microOp.immediate := microOpIn.immediate
    microOp.rdInfo.bits := microOpIn.physicalRd
    microOp.rdInfo.valid := microOpIn.physicalRd.orR
    microOp
  }
}