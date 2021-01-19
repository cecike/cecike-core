package cecike.core.common

import chisel3._
import cecike.core.common.Constants._

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
