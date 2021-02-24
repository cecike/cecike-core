package cecike.core.backend

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class BranchSnapshotBufferIO extends
  CommonRingBufferRequestIO(maxBranchCount, decodeWidth, decodeWidth) {}

// Allocate branch tag and store branch status
// the branch tag uses one-hot encoding.
class BranchSnapshotBuffer extends Module {
  require(isPow2(maxBranchCount))

  val io = IO(new BranchSnapshotBufferIO)

  val manager = Module(new RingBufferManager(maxBranchCount, decodeWidth, decodeWidth))
  manager.io.clear := false.B
  manager.io.req.bits := io.allocateReq
  manager.io.req.valid := true.B
  manager.io.deallocate := io.deallocateReq

  io.allocateResp := manager.io.resp
}
