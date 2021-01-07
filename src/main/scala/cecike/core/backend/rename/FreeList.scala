package cecike.core.backend.rename

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class FreeListIO extends Bundle {
  val allocateReq = Input(Vec(decodeWidth, Bool()))
  val allocateResp = Output(Valid(Vec(decodeWidth, UInt(physicalRegisterAddressWidth.W))))
  val deallocateReq = Input(Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W))))
}

class FreeList extends Module {
  val io = IO(new FreeListIO)

  val freeList = RegInit((~1.U(physicalRegisterNum.W)).asUInt)

  val freeListHalfWidth = physicalRegisterNum >> 1
  val freeListLo = freeList(freeListHalfWidth - 1, 0)
  val freeListHi = freeList(physicalRegisterNum - 1, freeListHalfWidth)

  // Generate allocate resp
  val allocateResp = Wire(Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W))))

  val allocateRespLo = BinaryPriorityEncoder(freeListLo)
  val allocateRespLoRev = ReversedBinaryPriorityEncoder(freeListLo)
  val allocateRespLoEq = allocateRespLo.bits === allocateRespLoRev.bits

  val allocateRespHi = BinaryPriorityEncoder(freeListHi)
  val allocateRespHiRev = ReversedBinaryPriorityEncoder(freeListHi)
  val allocateRespHiEq = allocateRespHi.bits === allocateRespHiRev.bits

  def connectAllocateResp(result: Valid[UInt], index: Int, hi: Boolean, mask: Bool = true.B): Unit = {
    allocateResp(index).bits := Cat(hi.B, result.bits)
    allocateResp(index).valid := mask && result.valid
  }
  connectAllocateResp(allocateRespLo, 0, false)
  connectAllocateResp(allocateRespHi, 1, true)
  connectAllocateResp(allocateRespLoRev, 2, false, !allocateRespLoEq)
  connectAllocateResp(allocateRespHiRev, 3, true, !allocateRespHiEq)

  for (i <- 0 until decodeWidth) {
    io.allocateResp.bits(i) := allocateResp(i).bits
  }
  io.allocateResp.valid := (allocateResp zip io.allocateReq)
    .map(p => p._1.valid || (!p._2))
    .reduce(_&_)

  val allocateMask = Mux(io.allocateResp.valid,
    (allocateResp zip io.allocateReq)
      .map(p => Mux(p._2, (1.U(physicalRegisterNum.W) << p._1.bits).asUInt, 0.U))
      .reduce(_|_), 0.U)
  val deallocateMask = io.deallocateReq
    .map(p => Mux(p.valid, (1.U(physicalRegisterNum.W) << p.bits).asUInt, 0.U))
    .reduce(_|_) & (~1.U(physicalRegisterNum.W)).asUInt

  val nextFreeList = (freeList & (~allocateMask).asUInt) | deallocateMask
  freeList := nextFreeList
}
