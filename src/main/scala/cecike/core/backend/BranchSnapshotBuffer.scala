package cecike.core.backend

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class BranchSnapshotBufferReadPort extends Bundle {
  val branchTag = Input(UInt(maxBranchCount.W))
  val valid = Output(Bool())
}

class BranchSnapshotBufferIO extends Bundle {
  val allocateReq = Input(Vec(decodeWidth, Bool()))
  val allocateResp = Output(Valid(Vec(decodeWidth, UInt(maxBranchCount.W))))
  val deallocateReq = Input(Vec(decodeWidth, Bool()))
}

// Allocate branch tag and store branch status
// the branch tag uses one-hot encoding.
class BranchSnapshotBuffer extends Module {
  require(isPow2(maxBranchCount))
  val snapshotCounterWidth = log2Ceil(maxBranchCount)

  val io = IO(new BranchSnapshotBufferIO)

  val manager = Module(new RingBufferManager(maxBranchCount, decodeWidth, decodeWidth))
  manager.io.req.bits := io.allocateReq
  manager.io.req.valid := true.B
  manager.io.deallocate := io.deallocateReq

  (io.allocateResp.bits zip manager.io.resp.bits).foreach(p => p._1 := UIntToOH(p._2))
  io.allocateResp.valid := manager.io.resp.valid
}
