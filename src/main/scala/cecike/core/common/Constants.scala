package cecike.core.common

import chisel3._
import chisel3.util._

object Constants {
  val xLen = 64 // From RISC-V Spec
  val wLen = 32

  val instructionLen = 32

  val logicalRegisterNum = 32
  val physicalRegisterNum = 128

  val logicalRegisterAddressWidth = log2Ceil(logicalRegisterNum)
  val physicalRegisterAddressWidth = log2Ceil(physicalRegisterNum)

  val decodeWidth = 2
  val issueWidth = 3

  val issueClusterNum = 2

  val maxBranchCount = 8
  val branchTagWidth = log2Ceil(maxBranchCount)

  val robBankNum = decodeWidth
  val robRowNum = 32
  val robEntryNum = robBankNum * robRowNum
  val robAddressWidth = log2Ceil(robEntryNum)
  val robRowAddressWidth = log2Ceil(robRowNum)
  val robBankAddressWidth = log2Ceil(robBankNum)

  val lsuQueueDepth = 16
  val lsuQueueAddressWidth = log2Ceil(lsuQueueDepth)

  val verboseTest = false

  object TF {
    val T = true.B
    val F = false.B
  }

  object InstructionType {
    val instTypeWidth = 3
    val IX = inst(0)
    val R = inst(1)
    val I = inst(2)
    val S = inst(3)
    val B = inst(4)
    val U = inst(5)
    val J = inst(6)

    def inst(data: Int) = {
      data.U(instTypeWidth.W)
    }

    def needRs2Instruction(data: UInt) = {
      data === R || data === S || data === B
    }
  }

  object FunctionUnitType {
    val fuTypeWidth = 4

    val FU_ALU = (1 << 0).U(fuTypeWidth.W)
    val FU_BRU = (1 << 1).U(fuTypeWidth.W)
    val FU_MDU = (1 << 2).U(fuTypeWidth.W)
    val FU_LSU = (1 << 3).U(fuTypeWidth.W)

    val FU_X = FU_ALU

    def typeMatch(a: UInt, b: UInt): Bool = {
      (a & b).orR()
    }

    def fuTypeCode(hasALU: Boolean = false,
                   hasBRU: Boolean = false,
                   hasMDU: Boolean = false,
                   hasLSU: Boolean = false): UInt = {
      var result = 0.U(fuTypeWidth.W)
      if (hasALU) {
        result |= FU_ALU
      }
      if (hasBRU) {
        result |= FU_BRU
      }
      if (hasMDU) {
        result |= FU_MDU
      }
      if (hasLSU) {
        result |= FU_LSU
      }
      result
    }
  }

  def functionUnitOpWidth = List(ALUOp.aluOpWidth, BRUOp.bruOpWidth, LSUOp.lsuWidth).max

  object ALUOp {
    val aluOpWidth = 5

    val ADD = op(0)
    val SUB = op(1)
    val SLT = op(2)
    val SLTU = op(3)
    val AND = op(4)
    val OR = op(5)
    val XOR = op(6)
    val SLL = op(7)
    val SRL = op(8)
    val SRA = op(9)
    val LUI = op(10)
    val AUIPC = op(11)

    val ADDW = op(12)
    val SUBW = op(13)
    val SLLW = op(14)
    val SRLW = op(15)
    val SRAW = op(16)

    def op(data: Int) = {
      data.U(aluOpWidth.W)
    }
  }

  object BRUOp {
    val bruOpWidth = 4

    val BX = op(0)
    val J = op(1)
    val EQ = op(2)
    val NE = op(3)
    val LT = op(4)
    val GE = op(5)
    val LTU = op(6)
    val GEU = op(7)

    def op(data: Int) = {
      data.U(bruOpWidth.W)
    }
  }

  object LSUOp {
    val lsuWidth = 5

    val LB = op(0)
    val LH = op(1)
    val LW = op(2)
    val LD = op(3)
    val LBU = op(4)
    val LHU = op(5)
    val LWU = op(6)

    val SB = op(8)
    val SH = op(9)
    val SW = op(10)
    val SD = op(11)


    def op(data: Int) = {
      data.U(lsuWidth.W)
    }
  }

  object MemorySize {
    val memSzWidth = 2
    val byte :: half :: word :: double :: Nil = Enum(4)
  }
}
