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
  val branchPredictionInfo = new BranchPredictionInfo

  val instType = UInt(InstructionType.instTypeWidth.W)
  val fuType = UInt(FunctionUnitType.fuTypeWidth.W)
  val fuOp = UInt(functionUnitOpWidth.W)

  val rs1Info = new SourceRegisterInfo
  val rs2Info = new SourceRegisterInfo
  val immediate = UInt(xLen.W)

  val rdInfo = UndirectionedValid(UInt(physicalRegisterAddressWidth.W))

  val robIndex = UInt(robAddressWidth.W)
}

object IssueMicroOp {
  def apply(microOpIn: MicroOp) = {
    val microOp = Wire(new IssueMicroOp)
    microOp.pc := microOpIn.pc

    microOp.branchTag := microOpIn.branchTag
    microOp.branchPredictionInfo := microOpIn.branchPredictionInfo

    microOp.instType := microOpIn.instType
    microOp.fuType := microOpIn.fuType
    microOp.fuOp := microOpIn.fuOp

    microOp.rs1Info.addr := microOpIn.physicalRs1
    microOp.rs1Info.busy := microOpIn.physicalRs1Busy

    microOp.rs2Info.addr := microOpIn.physicalRs2
    microOp.rs2Info.busy := microOpIn.physicalRs2Busy

    microOp.immediate := microOpIn.immediate

    microOp.rdInfo.bits := microOpIn.physicalRd
    microOp.rdInfo.valid := microOpIn.physicalRd.orR

    microOp.robIndex := microOpIn.robIndex

    microOp
  }
}