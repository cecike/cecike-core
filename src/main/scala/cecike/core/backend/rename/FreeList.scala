package cecike.core.backend.rename

import cecike.CecikeModule
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class FreeListIO extends Bundle {
  val flush = Input(Bool())
  val allocateReq = Input(Vec(decodeWidth, Bool()))
  val allocateResp = Output(Valid(Vec(decodeWidth, UInt(physicalRegisterAddressWidth.W))))
  val deallocateReq = Input(Vec(decodeWidth, UInt(physicalRegisterNum.W)))
  val persistReq = Input(Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W))))
}

class FreeList extends CecikeModule {
  val io = IO(new FreeListIO)

  val architecturalFreeList = RegInit((~1.U(physicalRegisterNum.W)).asUInt)
  val freeList = RegInit((~1.U(physicalRegisterNum.W)).asUInt)

  // Generate allocate resp
  val allocateResp = Wire(Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W))))

  val allocateList = freeList(physicalRegisterNum - 1, 0)
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

  when (!io.allocateResp.valid) {
    log("No more place to allocate")
  }

  val allocateMask = Mux(io.allocateResp.valid,
    (allocateResp zip io.allocateReq)
      .map(p => Mux(p._2, UIntToOH(p._1.bits, physicalRegisterNum), 0.U))
      .reduce(_|_), 0.U)
  val deallocateMask = io.deallocateReq
    .reduce(_|_) & (~1.U(physicalRegisterNum.W)).asUInt
  val persistMask = io.persistReq
    .map(p => Mux(p.valid, UIntToOH(p.bits, physicalRegisterNum), 0.U))
    .reduce(_|_)

  val nextArchitecturalFreeList = (architecturalFreeList & (~persistMask).asUInt) | deallocateMask
  val nextFreeList = (freeList & (~allocateMask).asUInt) | deallocateMask

  architecturalFreeList := nextArchitecturalFreeList
  freeList := Mux(io.flush, nextArchitecturalFreeList, nextFreeList)

  log("Freelist: %x", freeList)
  log("Architectural free list: %x", architecturalFreeList)
  log(allocateMask.orR(), "Allocate mask: %x", allocateMask)
  log(deallocateMask.orR(), "Deallocate mask: %x", deallocateMask)
  log(persistMask.orR(), "Persist mask: %x", persistMask)
}
