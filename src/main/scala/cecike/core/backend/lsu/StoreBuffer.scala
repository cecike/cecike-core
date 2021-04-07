package cecike.core.backend.lsu

import cecike.CecikeModule
import cecike.core.backend.register.RegisterFileWritePort
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.core.memory.{MemoryReadPort, MemoryWritePort, StoreInfo}
import cecike.core.memory.tlb.TLBQueryPort
import cecike.utils.RingBufferManager

class StoreBufferEntry extends Bundle {
  val valid = Bool()
  val commit = Bool()
  val info = new StoreInfo
  val robIndex = UInt(robAddressWidth.W)
}

class StoreBufferCheckExistencePort extends Bundle {
  val address = Output(UInt(xLen.W))
  val exist = Input(Bool())
}

class StoreBufferIO extends Bundle {
  val lsuEntry = DeqIO(new LSUEntry)

  val existencePort = Flipped(new StoreBufferCheckExistencePort)

  val flush = Input(Bool())
  val ableToCommit = Input(Bool())
  val storeCommit = Input(Bool())
  val readyROB = Valid(UInt(robAddressWidth.W))

  val storeInfo = EnqIO(new StoreInfo)
}

class StoreBuffer extends CecikeModule {
  val io = IO(new StoreBufferIO)

  val buffer = Reg(Vec(storeBufferDepth, new StoreBufferEntry))
  when (reset.asBool()) {
    buffer.foreach(_.valid := false.B)
    buffer.foreach(_.commit := false.B)
  }

  val head = RegInit(0.U(storeBufferAddressWidth.W))
  val commit = RegInit(0.U(storeBufferAddressWidth.W))
  val tail = RegInit(0.U(storeBufferAddressWidth.W))

  def empty = head === tail
  def full = (tail + 1.U) === head
  def nothingToCommit = commit === tail
  def nothingToWrite = head === commit

  io.existencePort.exist := buffer.map { p =>
    (p.valid || p.commit) &&
      p.info.addressInfo.address(xLen - 1, 3) === io.existencePort.address(xLen - 1, 3)
  }.reduce(_||_)

  io.lsuEntry.ready := !full && !io.flush

  when (io.flush) {
    tail := commit
    buffer.foreach(_.valid := false.B)
  } otherwise {
    when (io.lsuEntry.fire()) {
      val bufferTail = buffer(tail)
      bufferTail.valid := true.B
      bufferTail.info.addressInfo := io.lsuEntry.bits.aguInfo.address
      bufferTail.info.data := io.lsuEntry.bits.aguInfo.data
      bufferTail.robIndex := io.lsuEntry.bits.aguInfo.opInfo.robIndex
      tail := tail + 1.U
    }
  }

  io.readyROB.bits := buffer(commit).robIndex
  io.readyROB.valid := false.B
  when (io.ableToCommit && io.storeCommit && !nothingToCommit) {
    buffer(commit).commit := true.B
    commit := commit + 1.U
    io.readyROB.valid := true.B
  }

  io.storeInfo.valid := !nothingToWrite
  io.storeInfo.bits := buffer(head).info

  when (io.storeInfo.fire()) {
    buffer(head).valid := false.B
    buffer(head).commit := false.B
    head := head + 1.U
  }

  log(p"Head $head Commit $commit Tail $tail")
}
