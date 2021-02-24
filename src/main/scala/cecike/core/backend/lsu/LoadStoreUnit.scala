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
  val aguInfo = DecoupledIO(new AGUInfo)
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

  val queueManager = Module(new RingBufferManager(lsuQueueDepth, decodeWidth, decodeWidth))
  val queue = Reg(Vec(lsuQueueDepth, new LSUQueueEntry))
  // Init
  when (reset.asBool()) {
    queue.foreach { p =>
      p.status.allocated := false.B
    }
  }

  val dependencyMatrix = Reg(Vec(lsuQueueDepth, UInt(lsuQueueDepth.W)))
}
