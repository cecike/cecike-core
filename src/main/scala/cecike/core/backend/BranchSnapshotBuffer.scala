package cecike.core.backend

import cecike.core.common.BranchInfo
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class BranchSnapshotBufferIO extends Bundle {
  val allocateReq = Input(Vec(decodeWidth, Bool()))
  val allocateResp = Output(Valid(Vec(decodeWidth, UInt(branchTagWidth.W))))
  val deallocateReq = Input(Vec(decodeWidth, Bool()))
  val branchInfo = Input(new BranchInfo)
}

// Allocate branch tag and store branch status
// the branch tag uses one-hot encoding.
class BranchSnapshotBuffer extends Module {
  require(isPow2(maxBranchCount))

  val io = IO(new BranchSnapshotBufferIO)

  val manager = Module(new RingBufferManager(maxBranchCount, decodeWidth, decodeWidth))
  manager.io.req.bits := io.allocateReq
  manager.io.req.valid := true.B
  manager.io.deallocate := io.deallocateReq
  manager.io.restore := io.branchInfo.branchFlush

  io.allocateResp := manager.io.resp
}
