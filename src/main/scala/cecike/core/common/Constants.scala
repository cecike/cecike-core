package cecike.core.common

import chisel3.util._

object Constants {
  val xLen = 64 // From RISC-V Spec

  val logicalRegisterNum = 32
  val physicalRegisterNum = 128

  val logicalRegisterAddressWidth = log2Ceil(logicalRegisterNum)
  val physicalRegisterAddressWidth = log2Ceil(physicalRegisterNum)

  val decodeWidth = 4
  val issueWidth = 6

  val branchSnapshotCount = 8
  val branchTagWidth = log2Ceil(branchSnapshotCount)
}
