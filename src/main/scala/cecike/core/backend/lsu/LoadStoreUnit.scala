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
  val tlb = new TLBQueryPort
  val memoryRead = new MemoryReadPort
  val memoryWrite = new MemoryWritePort
}

class LoadStoreUnit extends Module {
  val io = IO(new LoadStoreUnitIO)

  // TODO: ...
  // Stage 1: take input from agu
  // Stage 2: translate virtual address to physical address - 1 cycle at least --> FSM_A // Done
  // Stage 3:
  //    For LOAD:
  //        Access cache and store buffer(8 entry) --> FSM_B
  //    For STORE:
  //        Add to store buffer is not full --> FSM_C
  //        Or block LSU to wait for a empty entry
}
