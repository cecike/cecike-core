package cecike.core

import chisel3.util._

object Constants {
  val xLen = 64 // From RISC-V Spec

  val physicalRegisterNum = 128
  val physicalRegisterAddressWidth = log2Ceil(physicalRegisterNum)

  val decodeWidth = 4

  val branchSnapshotCount = 8
  val branchTagWidth = log2Ceil(branchSnapshotCount)
}
