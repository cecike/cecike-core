package cecike.core.backend.lsu

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.core.memory.{MemoryReadPort, MemoryWritePort}
import cecike.core.memory.tlb.TLBQueryPort
import cecike.utils.RingBufferManager

class VirtualAddressTranslateFSMIO extends Bundle {
  val agu = Flipped(DecoupledIO(new AGUInfo))
  val tlb = new TLBQueryPort
  val res = DecoupledIO(new LSUEntry)
}

class VirtualAddressTranslateFSM extends Module {
  val io = IO(new VirtualAddressTranslateFSMIO)

  // default values
  io.res.valid := false.B

  io.tlb.virtualAddress.valid := false.B
  io.tlb.virtualAddress.bits := 0.U

  // to store info
  val lsuEntry = Reg(new LSUEntry)

  // state
  val s_idle :: s_shake :: s_wait :: s_send :: Nil = Enum(4)
  val state = RegInit(s_idle)

  // next state
  val nextState = WireDefault(s_idle)
  val eatNewAGU = WireDefault(false.B)
  val storeAddress = WireDefault(false.B)
  switch(state) {
    is(s_idle) {
      eat()
      when (io.agu.valid) {
        nextState := s_shake
      }
    }

    is(s_shake) {
      io.tlb.virtualAddress.valid := true.B
      io.tlb.virtualAddress.bits := lsuEntry.aguInfo.address.address

      when (io.tlb.virtualAddress.ready) {
        setStateWhenWait()
      }
      nextState := s_shake
    }

    is(s_wait) {
      setStateWhenWait()
    }

    is(s_send) {
      setStateWhenSend()
    }
  }
  state := nextState

  def eat() = {
    eatNewAGU := true.B
  }

  def setStateWhenSend() = {
    io.res.valid := true.B

    when (io.res.ready) {
      eat()
      when (io.agu.valid) {
        nextState := s_shake
      }
      nextState := s_idle
    }
    nextState := s_send
  }

  def setStateWhenWait() = {
    when (io.tlb.queryResult.valid) {
      storeAddress := true.B
      setStateWhenSend()
    }
    nextState := s_wait
  }

  io.agu.ready := eatNewAGU

  when (eatNewAGU && io.agu.valid) {
    lsuEntry.aguInfo := io.agu.bits
    lsuEntry.status.exception := false.B
  } otherwise when (storeAddress) {
    lsuEntry.aguInfo.address.address := io.tlb.queryResult.bits.physicalAddress
    lsuEntry.status.exception := !io.tlb.queryResult.bits.valid
  }

  // Output
  val result = Wire(new LSUEntry)
  result := lsuEntry
  when (storeAddress) {
    result.aguInfo.address.address := io.tlb.queryResult.bits.physicalAddress
    result.status.exception := !io.tlb.queryResult.bits.valid
  }
  io.res.bits := result

}
