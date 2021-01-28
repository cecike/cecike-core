package cecike.core.backend.decode

import chisel3._
import chisel3.util._
import cecike.core.common._
import cecike.core.common.Constants._
import cecike.core.common.Constants.TF._
import cecike.core.common.Constants.InstructionType._
import cecike.core.common.Constants.FunctionUnitType._
import cecike.utils._

trait InstructionTable {
  def check(cond: Bool, data: ControlSignal): ControlSignal = {
    Mux(cond, data, CS.err())
  }

  def apply(i: UInt): ControlSignal
}

trait CommonInstructionTable extends InstructionTable {
  def opcode(i: UInt) = i(6, 2)
  def funct3(i: UInt) = i(14, 12)
  def funct7(i: UInt) = i(31, 25)

  def decode(i: UInt): ControlSignal

  override def apply(i: UInt) = {
    check(i(1, 0).andR(), decode(i))
  }
}

object RvBranchInstructionTable extends CommonInstructionTable {
  import cecike.core.common.Constants.BRUOp._

  override def decode(i: UInt) = {
    val table = Array(
      "b000" -> EQ,
      "b001" -> NE,
      "b100" -> LT,
      "b101" -> GE,
      "b110" -> LTU,
      "b111" -> GEU
    ).map(p => (BitPat(p._1), p._2))
    val (valid, fuOp) = BinaryMuxLookUp(funct3(i), table)

    val cs = Wire(new ControlSignal)

    cs.instType := B
    cs.fuType := FU_BRU
    cs.fuOp := fuOp
    cs.rs1Valid := T
    cs.rs2Valid := T
    cs.rdValid := F
    cs.immediate := DontCare

    check(valid, cs)
  }
}

object RvOpInstructionTable extends CommonInstructionTable {
  import cecike.core.common.Constants.ALUOp._

  override def decode(i: UInt) = {
    val table = Array(
      "b000" -> Mux(i(5) && i(30), SUB, ADD),
      "b001" -> SLL,
      "b010" -> SLT,
      "b011" -> SLTU,
      "b100" -> XOR,
      "b101" -> Mux(i(30), SRA, SRL),
      "b110" -> OR,
      "b111" -> AND
    ).map(p => (BitPat(p._1), p._2))
    val fuOp = BinaryMuxLookUp(funct3(i), table)._2

    val cs = Wire(new ControlSignal)

    cs.instType := Mux(i(5), R, I)
    cs.fuType := FU_ALU
    cs.fuOp := fuOp
    cs.rs1Valid := T
    cs.rs2Valid := i(5)
    cs.rdValid := T
    cs.immediate := DontCare

    cs
  }
}

object RvOp32InstructionTable extends CommonInstructionTable {
  import cecike.core.common.Constants.ALUOp._

  override def decode(i: UInt) = {
    val table = Array(
      "b000" -> Mux(i(5) && i(30), SUBW, ADDW),
      "b001" -> SLLW,
      "b101" -> Mux(i(30), SRAW, SRLW),
    ).map(p => (BitPat(p._1), p._2))
    val (valid, fuOp) = BinaryMuxLookUp(funct3(i), table)

    val cs = Wire(new ControlSignal)

    cs.instType := Mux(i(5), R, I)
    cs.fuType := FU_ALU
    cs.fuOp := fuOp
    cs.rs1Valid := T
    cs.rs2Valid := i(5)
    cs.rdValid := T
    cs.immediate := DontCare

    check(valid, cs)
  }
}

object Rv64InstructionTable extends CommonInstructionTable {
  override def decode(i: UInt) = {
    val table = Array(
      "b01101" -> CS.ok(U, FU_ALU, ALUOp.LUI, F, F, T),
      "b00101" -> CS.ok(U, FU_ALU, ALUOp.AUIPC, F, F, T),
      "b11000" -> RvBranchInstructionTable(i),
      "b0?100" -> RvOpInstructionTable(i),
      "b0?110" -> RvOp32InstructionTable(i)
    ).map(p => (BitPat(p._1), p._2))

    val (valid, cs) = BinaryMuxLookUp(opcode(i), table)
    check(valid, cs)
  }
}
