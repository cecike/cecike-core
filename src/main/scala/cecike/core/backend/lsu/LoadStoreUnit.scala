package cecike.core.backend.lsu

import cecike.core.backend.register.RegisterFileWritePort
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.core.memory.{MemoryReadPort, MemoryWritePort}
import cecike.core.memory.tlb.TLBQueryPort
import cecike.utils.RingBufferManager

class LoadStoreInfo extends Bundle {
  val aguInfo = EnqIO(new AGUInfo)
  val readyROB = Flipped(Valid(UInt(robAddressWidth.W)))
  val readyRd = Flipped(Valid(UInt(physicalRegisterAddressWidth.W)))
  val rdWrite = new RegisterFileWritePort
}

class LoadStoreUnitIO extends Bundle {
  val agu = Flipped(new LoadStoreInfo)
  val flush = Input(Bool())
  val tlb = new TLBQueryPort
  val memoryRead = new MemoryReadPort
  val memoryWrite = new MemoryWritePort
  val storeCommit = Input(Bool())
}

class LoadStoreUnit extends Module {
  val io = IO(new LoadStoreUnitIO)

  // TODO: ...
  // Stage 1: take input from agu
  // Stage 2: translate virtual address to physical address - 1 cycle at least --> FSM_A // Done
  // Stage 3:
  //    For LOAD:
  //        Access cache and store buffer(8 entry) --> FSM_B // Done
  //    For STORE:g
  //        Add to store buffer is not full --> FSM_C
  //        Or block LSU to wait for a empty entry
  // Load has a higher priority
  val addressTranslateFSM = Module(new VirtualAddressTranslateFSM)
  addressTranslateFSM.io.flush := io.flush
  addressTranslateFSM.io.agu <> io.agu.aguInfo
  addressTranslateFSM.io.tlb <> io.tlb

  val storeBuffer = Module(new StoreBuffer)
  val loadFromMemoryFSM = Module(new LoadFromMemoryFSM)

  storeBuffer.io.flush := io.flush
  loadFromMemoryFSM.io.flush := io.flush

  loadFromMemoryFSM.io.lsuEntry.bits := addressTranslateFSM.io.res.bits
  storeBuffer.io.lsuEntry.bits := addressTranslateFSM.io.res.bits

  loadFromMemoryFSM.io.lsuEntry.valid := false.B
  storeBuffer.io.lsuEntry.valid := false.B
  when (addressTranslateFSM.io.res.bits.aguInfo.load) {
    loadFromMemoryFSM.io.lsuEntry.valid := addressTranslateFSM.io.res.valid
    addressTranslateFSM.io.res.ready := loadFromMemoryFSM.io.lsuEntry.ready
  } otherwise {
    storeBuffer.io.lsuEntry.valid := addressTranslateFSM.io.res.valid
    addressTranslateFSM.io.res.ready := storeBuffer.io.lsuEntry.ready
  }

  storeBuffer.io.ableToCommit := !loadFromMemoryFSM.io.readyROB.valid
  storeBuffer.io.storeCommit := io.storeCommit
  storeBuffer.io.storeInfo <> io.memoryWrite.storeInfo

  when (loadFromMemoryFSM.io.readyROB.valid) {
    io.agu.readyROB := loadFromMemoryFSM.io.readyROB
  } otherwise {
    io.agu.readyROB := storeBuffer.io.readyROB
  }

  loadFromMemoryFSM.io.memoryRead <> io.memoryRead
  loadFromMemoryFSM.io.storeBuffer <> storeBuffer.io.existencePort

  io.agu.readyRd := loadFromMemoryFSM.io.readyRd
  io.agu.rdWrite := loadFromMemoryFSM.io.rdWrite
}
