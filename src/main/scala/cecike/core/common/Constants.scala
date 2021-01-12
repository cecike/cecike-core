package cecike.core.common

import chisel3._
import chisel3.util._

object Constants {
  val useSmallCecike = false

  val xLen = 64 // From RISC-V Spec
  val wLen = 32

  val instructionLen = 32

  val logicalRegisterNum = 32
  val physicalRegisterNum = withSmallOption(128, 64)

  val logicalRegisterAddressWidth = log2Ceil(logicalRegisterNum)
  val physicalRegisterAddressWidth = log2Ceil(physicalRegisterNum)

  val decodeWidth = withSmallOption(4, 2)
  val issueWidth = withSmallOption(6, 3)

  val branchSnapshotCount = 8
  val branchTagWidth = log2Ceil(branchSnapshotCount)

  def withSmallOption[T](data: T, opt: T) = {
    if (useSmallCecike) {
      opt
    } else {
      data
    }
  }

  object FunctionUnitType {
    val fuTypeWidth = 4
    val FU_X = BitPat.dontCare(fuTypeWidth)
    val FU_ALU = (1 << 0).U(fuTypeWidth.W)
    val FU_BRU = (1 << 1).U(fuTypeWidth.W)
    val FU_MDU = (1 << 2).U(fuTypeWidth.W)
    val FU_LSU = (1 << 3).U(fuTypeWidth.W)
  }

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
