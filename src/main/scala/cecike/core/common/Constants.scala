package cecike.core.common

import chisel3._
import chisel3.util._

object Constants {
  val hasLog = true.B

  val xLen = 64 // From RISC-V Spec
  val wLen = 32

  val instructionLen = 32

  val logicalRegisterNum = 32
  val physicalRegisterNum = 64

  val logicalRegisterAddressWidth = log2Ceil(logicalRegisterNum)
  val physicalRegisterAddressWidth = log2Ceil(physicalRegisterNum)

  val decodeWidth = 2

  val commonIssueWidth = 2
  val lsuIssueWidth = 1
  val issueWidth = commonIssueWidth + lsuIssueWidth

  val issueClusterNum = 2

  val maxBranchCount = 8
  val branchTagWidth = log2Ceil(maxBranchCount)

  val robBankNum = decodeWidth
  val robRowNum = 16
  val robEntryNum = robBankNum * robRowNum
  val robAddressWidth = log2Ceil(robEntryNum)
  val robRowAddressWidth = log2Ceil(robRowNum)
  val robBankAddressWidth = log2Ceil(robBankNum)

  val commonQueueDepth = 8

  val lsuQueueDepth = 16
  val lsuQueueAddressWidth = log2Ceil(lsuQueueDepth)

  val storeBufferDepth = 8
  val storeBufferAddressWidth = log2Ceil(storeBufferDepth)

  val cacheLineWidth = 64
  val cacheLineAddressWidth = log2Ceil(cacheLineWidth)

  val pcInitLitValue = BigInt("80000000", 16)
  val pcInitValue = pcInitLitValue.U(xLen.W)

  val hasBranchPredictor = false.B
  val globalHistoryWidth = 9

  val verboseTest = true

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
    val JR = op(8)

    def op(data: Int) = {
      data.U(bruOpWidth.W)
    }
  }

  object MDUOp {
    val mduOpWidth = 4

    val MUL = op(0)
    val MULH = op(1)
    val MULHU = op(2)
    val MULHSU = op(3)
    val MULW = op(4)

    val DIV = op(8)
    val DIVU = op(9)
    val REM = op(10)
    val REMU = op(11)
    val DIVW = op(12)
    val DIVUW = op(13)
    val REMW = op(14)
    val REMUW = op(15)


    def op(data: Int) = {
      data.U(mduOpWidth.W)
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

    def opIsStore(op: UInt) = {
      op(3)
    }

    def op(data: Int) = {
      data.U(lsuWidth.W)
    }
  }

  object MemorySize {
    val memSzWidth = 2
    val byte :: half :: word :: double :: Nil = Enum(4)
  }
}
