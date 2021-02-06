package cecike.core.backend.rename

import cecike.core.common.BranchInfo
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class FreeListIO extends Bundle {
  val allocateReq = Input(Vec(decodeWidth, Bool()))
  val allocateResp = Output(Valid(Vec(decodeWidth, UInt(physicalRegisterAddressWidth.W))))
  val deallocateReq = Input(Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W))))
  val branchInfo = Input(new BranchInfo)
  val newBranch = Flipped(Vec(decodeWidth, Valid(UInt(branchTagWidth.W))))
}

class FreeList extends Module {
  val io = IO(new FreeListIO)

  val branchFlush = io.branchInfo.branchFlush
  val freeListSnapshot = Reg(Vec(maxBranchCount, UInt(physicalRegisterNum.W)))
  val currentFreeList = RegInit((~1.U(physicalRegisterNum.W)).asUInt)

  // Generate allocate resp
  val allocateResp = Wire(Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W))))

  val allocateList = currentFreeList(physicalRegisterNum - 1, 0)
  val allocateRespLo = BinaryPriorityEncoder(allocateList)
  val allocateRespLoRev = ReversedBinaryPriorityEncoder(allocateList)
  val allocateRespLoEq = allocateRespLo.bits === allocateRespLoRev.bits

  def connectAllocateResp(result: Valid[UInt], index: Int, mask: Bool = true.B): Unit = {
    allocateResp(index).bits := result.bits
    allocateResp(index).valid := mask && result.valid
  }
  connectAllocateResp(allocateRespLo, 0)
  connectAllocateResp(allocateRespLoRev, 1, !allocateRespLoEq)

  for (i <- 0 until decodeWidth) {
    io.allocateResp.bits(i) := allocateResp(i).bits
  }
  io.allocateResp.valid := (allocateResp zip io.allocateReq)
    .map(p => p._1.valid || (!p._2))
    .reduce(_&_)

  val allocateMaskArray = (allocateResp zip io.allocateReq)
    .map(p => Mux(p._2, UIntToOH(p._1.bits, physicalRegisterNum), 0.U))

  val partialAllocateMaskArray = Wire(Vec(decodeWidth, UInt(physicalRegisterNum.W)))
  partialAllocateMaskArray(0) := 0.U

  for (i <- 1 until decodeWidth) {
    partialAllocateMaskArray(i) := partialAllocateMaskArray(i - 1) | allocateMaskArray(i - 1)
  }

  val allocateMask = Mux(io.allocateResp.valid, allocateMaskArray.reduce(_|_), 0.U)
  val deallocateMask = io.deallocateReq
    .map(p => Mux(p.valid, UIntToOH(p.bits, physicalRegisterNum), 0.U))
    .reduce(_|_) & (~1.U(physicalRegisterNum.W)).asUInt

  for (i <- 0 until decodeWidth) {
    val nb = io.newBranch(i)
    when (nb.valid) {
      freeListSnapshot(nb.bits) := partialAllocateMaskArray(i) | deallocateMask
    }
  }

  for (i <- 0 until maxBranchCount) {
    val hasNB = io.newBranch.map(p => p.valid && p.bits === i.U).reduce(_||_)
    when (!hasNB) {
      freeListSnapshot(i) := freeListSnapshot(i) | deallocateMask
    }
  }

  val nextFreeList = Mux(branchFlush.valid, freeListSnapshot(branchFlush.bits),
    currentFreeList & (~allocateMask).asUInt) | deallocateMask
  currentFreeList := nextFreeList
}
