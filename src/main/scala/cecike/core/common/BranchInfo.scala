package cecike.core.common

import cecike.core.common.Constants._
import chisel3._

class BranchPredictionInfo extends Bundle {
  val taken = Bool()
  val dest = UInt(xLen.W)
}

class BranchInfo extends Bundle {
  val tag = UInt(maxBranchCount.W)
  val taken = Bool()
  val mispredicted = Bool()
  val dest = UInt(xLen.W)
}

// TODO: Move to module file when implement the BROB
class BranchROBReadPort extends Bundle {
  val branchTag = Input(UInt(maxBranchCount.W))
  val valid = Output(Bool())
}