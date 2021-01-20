package cecike.core.common

import chisel3._
import chisel3.util._

object UseSmallCecikeOption {
  val YES = true
  val NO = false
}

object Constants {
  val useSmallCecike = UseSmallCecikeOption.NO

  val xLen = 64 // From RISC-V Spec
  val wLen = 32

  val instructionLen = 32

  val logicalRegisterNum = 32
  val physicalRegisterNum = withSmallOption(128, 64)

  val logicalRegisterAddressWidth = log2Ceil(logicalRegisterNum)
  val physicalRegisterAddressWidth = log2Ceil(physicalRegisterNum)

  val decodeWidth = withSmallOption(4, 2)
  val issueWidth = withSmallOption(6, 3)

  val maxBranchCount = 8

  val robBankNum = decodeWidth
  val robRowNum = 24
  val robEntryNum = robBankNum * robRowNum
  val robAddressWidth = log2Ceil(robEntryNum)

  val verboseTest = false

  def withSmallOption[T](data: T, opt: T) = {
    if (useSmallCecike) {
      opt
    } else {
      data
    }
  }

  object InstructionType {
    val instTypeWidth = 3
    val X = inst(0)
    val R = inst(1)
    val I = inst(2)
    val S = inst(3)
    val B = inst(4)
    val U = inst(5)
    val J = inst(6)

    def inst(data: Int) = {
      data.U(instTypeWidth.W)
    }

    def isRegToRegInstruction(data: UInt) = {
      data === R || data === S || data === B
    }
  }

  object FunctionUnitType {
    val fuTypeWidth = 4
    val FU_X = BitPat.dontCare(fuTypeWidth)
    val FU_ALU = (1 << 0).U(fuTypeWidth.W)
    val FU_BRU = (1 << 1).U(fuTypeWidth.W)
    val FU_MDU = (1 << 2).U(fuTypeWidth.W)
    val FU_LSU = (1 << 3).U(fuTypeWidth.W)
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

  def functionUnitOpWidth = List(ALUOp.aluOpWidth, BRUOp.bruOpWidth).max

  object ALUOp {
    val aluOpWidth = 4

    val X = BitPat.dontCare(aluOpWidth)
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

    val ADDW = op(11)
    val SUBW = op(12)
    val SLLW = op(13)
    val SRLW = op(14)
    val SRAW = op(15)

    def op(data: Int) = {
      data.U(aluOpWidth.W)
    }
  }

  object BRUOp {
    val bruOpWidth = 3

    val X = op(0)
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
}
