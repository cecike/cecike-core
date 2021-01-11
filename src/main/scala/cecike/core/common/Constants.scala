package cecike.core.common

import chisel3.util._

object Constants {
  val useSmallCecike = false

  val xLen = 64 // From RISC-V Spec

  val instructionLen = 32

  val logicalRegisterNum = 32
  val physicalRegisterNum = if (useSmallCecike) 64 else 128

  val logicalRegisterAddressWidth = log2Ceil(logicalRegisterNum)
  val physicalRegisterAddressWidth = log2Ceil(physicalRegisterNum)

  val decodeWidth = if (useSmallCecike) 2 else 4
  val issueWidth = if (useSmallCecike) 3 else 6

  val branchSnapshotCount = 8
  val branchTagWidth = log2Ceil(branchSnapshotCount)
}
