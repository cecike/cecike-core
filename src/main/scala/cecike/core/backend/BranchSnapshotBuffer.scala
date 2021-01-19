package cecike.core.backend

import cecike.core.common.BranchInfo
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
  val readReq = new BranchSnapshotBufferReadPort
  val branchInfo = Input(new BranchInfo)
}

// Allocate branch tag and store branch status
// the branch tag uses one-hot encoding.
class BranchSnapshotBuffer extends Module {
  require(isPow2(maxBranchCount))
  val snapshotCounterWidth = log2Ceil(maxBranchCount)

  val io = IO(new BranchSnapshotBufferIO)

  val snapshotValid = RegInit(0.U(maxBranchCount.W))
  val snapshotHead = RegInit(0.U(snapshotCounterWidth.W))
  val snapshotTail = RegInit(0.U(snapshotCounterWidth.W))

  def empty() = {
    snapshotHead === snapshotTail
  }

  def full() = {
    snapshotTail + 1.U === snapshotHead
  }

  val allocate = Wire(Vec(decodeWidth, UInt(maxBranchCount.W)))
  val allocatedCount = Wire(Vec(decodeWidth + 1, UInt(snapshotCounterWidth.W)))
  val allocateValid = Wire(Vec(decodeWidth + 1, Bool()))

  allocatedCount(0) := snapshotTail
  allocateValid(0) := !full()
  for (i <- 0 until decodeWidth) {
    allocate(i) := Mux(io.allocateReq(i), UIntToOH(allocatedCount(i)), 0.U)
    allocatedCount(i + 1) := allocatedCount(i) + Mux(io.allocateReq(i), 1.U, 0.U)
    allocateValid(i + 1) := allocateValid(i) && allocatedCount(i + 1) === snapshotHead
  }
  io.allocateResp.bits := allocate
  io.allocateResp.valid := allocateValid(decodeWidth)

  when (allocateValid(decodeWidth)) {
    snapshotTail := allocatedCount(decodeWidth)
  }

  snapshotHead := snapshotHead + PopCount(io.deallocateReq)

  val snapshotAllocateMask = allocate.reduce(_|_)

  val tag = BinaryOHToUInt(io.branchInfo.tag)
  val tagMask = (~0.U(maxBranchCount.W) << tag).asUInt
  val headMask = (~(~0.U(maxBranchCount) << snapshotHead)).asUInt
  val invalidMaskTagBigger = tagMask | headMask
  val invalidMaskTagSmaller = tagMask & headMask
  val snapshotInvalidMask = (~Mux(io.branchInfo.valid && io.branchInfo.mispredicted,
    Mux(tag > snapshotHead, invalidMaskTagBigger, invalidMaskTagSmaller), 0.U)).asUInt()(maxBranchCount - 1, 0)
  snapshotValid := (snapshotValid | snapshotAllocateMask) & snapshotInvalidMask

  io.readReq.valid := snapshotValid(BinaryOHToUInt(io.readReq.branchTag))
}