package cecike.core.backend.lsu

import cecike.core.backend.register.RegisterFileWritePort
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.core.memory.{MemoryReadPort, MemoryWritePort}
import cecike.core.memory.tlb.TLBQueryPort
import cecike.utils.RingBufferManager

class LoadFromMemoryFSMIO extends Bundle {
  val lsuEntry = DeqIO(new LSUEntry)

  val storeBuffer = new StoreBufferCheckExistencePort
  val memoryRead = new MemoryReadPort

  val readyROB = Valid(UInt(robAddressWidth.W))
  val readyRd = Valid(UInt(physicalRegisterAddressWidth.W))
  val rdWrite = Flipped(new RegisterFileWritePort)
}

class LoadFromMemoryFSM extends Module {
  val io = IO(new LoadFromMemoryFSMIO)

  // default values
  io.lsuEntry.ready := false.B

  io.storeBuffer.address := 0.U

  io.memoryRead.addressInfo.valid := false.B
  io.memoryRead.addressInfo.bits.address := 0.U
  io.memoryRead.addressInfo.bits.size := 0.U
  io.memoryRead.data.ready := false.B

  io.readyRd.valid := false.B
  io.readyRd.bits := 0.U

  io.readyROB.valid := false.B
  io.readyROB.bits := 0.U

  io.rdWrite.valid := false.B
  io.rdWrite.addr := 0.U
  io.rdWrite.data := 0.U

  // to store info
  val lsuEntry = Reg(new LSUEntry)

  val s_idle :: s_shake :: s_wait :: Nil = Enum(3)

  val state = RegInit(s_idle)

  val nextState = WireDefault(s_idle)

  switch(state) {
    is (s_idle) {
      eat()
      when (io.lsuEntry.fire()) {
        shakeFromInput()
        shakeNextState()
      }
    }

    is (s_shake) {
      shakeFromRegister()
      shakeNextState()
    }

    is (s_wait) {
      io.memoryRead.data.ready := true.B
      when (io.memoryRead.data.fire()) {
        io.readyRd.valid := true.B
        io.readyRd.bits := lsuEntry.aguInfo.opInfo.rdInfo.bits

        io.readyROB.valid := true.B
        io.readyROB.bits := lsuEntry.aguInfo.opInfo.robIndex

        io.rdWrite.valid := false.B
        io.rdWrite.addr := lsuEntry.aguInfo.opInfo.rdInfo.bits
        io.rdWrite.data := io.memoryRead.data.bits

        eat()
        when (io.lsuEntry.fire()) {
          shakeFromInput()
          shakeNextState()
        } otherwise {
          nextState := s_idle
        }
      } otherwise {
        nextState := s_wait
      }
    }
  }

  def eat(): Unit = {
    io.lsuEntry.ready := true.B
  }

  def shakeFromInput(): Unit = {
    io.memoryRead.addressInfo.valid := !io.storeBuffer.exist
    io.memoryRead.addressInfo.bits := io.lsuEntry.bits.aguInfo.address
    io.storeBuffer.address := io.lsuEntry.bits.aguInfo.address.address
  }

  def shakeFromRegister(): Unit = {
    io.memoryRead.addressInfo.valid := !io.storeBuffer.exist
    io.memoryRead.addressInfo.bits := lsuEntry.aguInfo.address
    io.storeBuffer.address := lsuEntry.aguInfo.address.address
  }

  def shakeNextState(): Unit = {
    when (io.memoryRead.addressInfo.fire()) {
      nextState := s_wait
    } otherwise {
      nextState := s_shake
    }
  }
}
