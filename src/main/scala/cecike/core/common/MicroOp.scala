package cecike.core.common

import chisel3._
import cecike.core.common.Constants._
import cecike.utils._

class MicroOp extends Bundle {
  val valid = Bool()
  val pc = UInt(xLen.W)
  val instruction = UInt(instructionLen.W)

  // TODO: Detailed design in branch prediction
  val branchTag = UInt(maxBranchCount.W)
  val branchPredictionInfo = new BranchPredictionInfo

  // TODO: Generate these signals in decode stage
  val instType = UInt(InstructionType.instTypeWidth.W)
  val fuType = UInt(FunctionUnitType.fuTypeWidth.W)
  val fuOp = UInt(functionUnitOpWidth.W)

  // TODO: Generate these signals in decode stage
  val rs1Valid = Bool()
  val rs2Valid = Bool()
  val rdValid = Bool()

  val immediate = UInt(xLen.W)

  def rs1(): UInt = Mux(valid && rs1Valid, instruction(19, 15), 0.U)
  def rs2(): UInt = Mux(valid && rs2Valid, instruction(24, 20), 0.U)
  def rd(): UInt = Mux(valid && rdValid, instruction(11, 7), 0.U)
  def opcode(): UInt = instruction(6, 0)
  def funct3(): UInt = instruction(14, 12)
  def funct7(): UInt = instruction(31, 25)
  
  val orderInfo = new OrderInfo

  val physicalRs1 = UInt(physicalRegisterAddressWidth.W)
  val physicalRs1Busy = Bool()
  val physicalRs2 = UInt(physicalRegisterAddressWidth.W)
  val physicalRs2Busy = Bool()
  val physicalRd = UInt(physicalRegisterAddressWidth.W)
  val oldPhysicalRd = UInt(physicalRegisterAddressWidth.W)

  val robIndex = UInt(robAddressWidth.W)
}

object MicroOp {
  def apply() = {
    val microOp = WireDefault(new MicroOp, DontCare)

    microOp.instType := InstructionType.X
    microOp.fuType := FunctionUnitType.FU_ALU

    microOp
  }
}

class SourceRegisterInfo extends Bundle {
  val busy = Bool()
  val addr = UInt(physicalRegisterAddressWidth.W)
}

class IssueMicroOp extends Bundle {
  // Micro Op Info
  val pc = UInt(xLen.W)

  val branchTag = UInt(maxBranchCount.W)
  val branchPredictionInfo = new BranchPredictionInfo

  val instType = UInt(InstructionType.instTypeWidth.W)
  val fuType = UInt(FunctionUnitType.fuTypeWidth.W)
  val fuOp = UInt(functionUnitOpWidth.W)

  val rs1Info = new SourceRegisterInfo
  val rs2Info = new SourceRegisterInfo
  val immediate = UInt(xLen.W)

  val rdInfo = UndirectionalValid(UInt(physicalRegisterAddressWidth.W))

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
    microOp.rdInfo.valid := microOpIn.rdValid

    microOp.robIndex := microOpIn.robIndex

    microOp
  }
}

class ROBMicroOp extends Bundle {
  val valid = Bool()

  // Status
  // TODO: add exceptions
  val done = Bool()

  val branchTag = UInt(maxBranchCount.W)

  // To update map table
  val rdValid = Bool()
  val logicalRd = UInt(logicalRegisterAddressWidth.W)
  val physicalRd = UInt(physicalRegisterAddressWidth.W)
  val oldPhysicalRd = UInt(physicalRegisterAddressWidth.W)

  // To recover from ...
  val isBranchOp = Bool()
  val branchInfo = new BranchInfo
}

object ROBMicroOp extends Bundle {
  def apply(microOpIn: MicroOp) = {
    val microOp = Wire(new ROBMicroOp)
    microOp.valid := microOpIn.valid

    microOp.done := false.B
    microOp.branchTag := microOpIn.branchTag

    microOp.rdValid := microOpIn.rdValid
    microOp.logicalRd := microOpIn.rd()
    microOp.physicalRd := microOpIn.physicalRd
    microOp.oldPhysicalRd := microOpIn.oldPhysicalRd
    microOp.isBranchOp := (microOpIn.fuType & FunctionUnitType.FU_BRU).orR()
    microOp.branchInfo := DontCare

    microOp
  }
}