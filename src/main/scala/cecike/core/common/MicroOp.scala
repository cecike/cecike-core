package cecike.core.common

import chisel3._
import cecike.core.common.Constants._

class BranchPredictionResult extends Bundle {
  val taken = Bool()
  val dest = UInt(xLen.W)
}

class MicroOp extends Bundle {
  val valid = Bool()
  val pc = UInt(xLen.W)
  val instruction = UInt(instructionLen.W)

  // TODO: Detailed design in branch prediction
  val branchTag = UInt(branchTagWidth.W)
  val branchPredictionResult = new BranchPredictionResult

  // TODO: Generate these signals in decode stage
  val fuType = UInt(FunctionUnitType.fuTypeWidth.W)
  val fuOp = UInt(functionUnitOpWidth.W)

  // TODO: Generate these signals in decode stage
  // NOTE: register 0 is considered invalid here
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
}

object MicroOp {
  def apply() = {
    val microOp = Wire(new MicroOp)
    microOp.valid := false.B
    microOp.pc := 0.U
    microOp.instruction := 0.U

    microOp.branchTag := 0.U
    microOp.branchPredictionResult.taken := false.B
    microOp.branchPredictionResult.dest := 0.U

    microOp.fuType := FunctionUnitType.FU_ALU
    microOp.fuOp := 0.U

    microOp.immediate := 0.U
    microOp.rs1Valid := false.B
    microOp.rs2Valid := false.B
    microOp.rdValid := false.B
    microOp.physicalRs1 := 0.U
    microOp.physicalRs2 := 0.U
    microOp.physicalRd := 0.U
    microOp.physicalRs1Busy := false.B
    microOp.physicalRs2Busy := false.B
    microOp.oldPhysicalRd := 0.U
    microOp
  }
}