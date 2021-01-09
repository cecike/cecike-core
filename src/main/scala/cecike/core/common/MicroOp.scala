package cecike.core.common

import chisel3._
import cecike.core.common.Constants._

class MicroOp extends Bundle {
  val instruction = UInt(instructionLen.W)

  // TODO: Generate these signals in decode stage
  // NOTE: register 0 is considered invalid here
  val rs1Valid = Bool()
  val rs2Valid = Bool()
  val rdValid = Bool()

  def rs1(): UInt = Mux(rs1Valid, instruction(19, 15), 0.U)
  def rs2(): UInt = Mux(rs2Valid, instruction(24, 20), 0.U)
  def rd(): UInt = Mux(rdValid, instruction(11, 7), 0.U)
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
    microOp.instruction := 0.U
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