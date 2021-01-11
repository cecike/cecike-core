package cecike.core.common

import chisel3._
import chisel3.util._

object Constants {
  val useSmallCecike = false

  val xLen = 64 // From RISC-V Spec
  val wLen = 32

  val instructionLen = 32

  val logicalRegisterNum = 32
  val physicalRegisterNum = if (useSmallCecike) 64 else 128

  val logicalRegisterAddressWidth = log2Ceil(logicalRegisterNum)
  val physicalRegisterAddressWidth = log2Ceil(physicalRegisterNum)

  val decodeWidth = if (useSmallCecike) 2 else 4
  val issueWidth = if (useSmallCecike) 3 else 6

  val branchSnapshotCount = 8
  val branchTagWidth = log2Ceil(branchSnapshotCount)

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
}
