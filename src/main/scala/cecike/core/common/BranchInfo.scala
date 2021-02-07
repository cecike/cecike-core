package cecike.core.common

import cecike.core.common.Constants._
import chisel3._

class BranchPredictionInfo extends Bundle {
  val taken = Bool()
  val dest = UInt(xLen.W)
}

class BranchInfo extends Bundle {
  val valid = Bool()
  val robIndex = UInt(robAddressWidth.W)
  val tag = UInt(branchTagWidth.W)
  val taken = Bool()
  val mispredicted = Bool()
  val dest = UInt(xLen.W)
}
