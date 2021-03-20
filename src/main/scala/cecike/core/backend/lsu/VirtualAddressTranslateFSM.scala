package cecike.core.backend.lsu

import cecike.CecikeModule
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.core.memory.{MemoryReadPort, MemoryWritePort}
import cecike.core.memory.tlb.TLBQueryPort
import cecike.utils.RingBufferManager

class VirtualAddressTranslateFSMDebugIO extends Bundle {
  val state = UInt(2.W)
  val nextState = UInt(2.W)
}

class VirtualAddressTranslateFSMIO extends Bundle {
  val agu = DeqIO(new AGUInfo)
  val flush = Input(Bool())
  val tlb = new TLBQueryPort
  val res = EnqIO(new LSUEntry)
  val debug = Output(new VirtualAddressTranslateFSMDebugIO)
}

class VirtualAddressTranslateFSM extends CecikeModule {
  val io = IO(new VirtualAddressTranslateFSMIO)

  // default values
  io.res.valid := false.B

  io.tlb.virtualAddress.valid := false.B
  io.tlb.virtualAddress.bits := 0.U

  // to store info
  val lsuEntry = Reg(new LSUEntry)

  // state
  val s_idle :: s_shake :: s_wait :: s_send :: s_flush :: Nil = Enum(5)
  val state = RegInit(s_idle)

  // next state
  val nextState = WireDefault(s_idle)
  val eatNewAGU = WireDefault(false.B)
  val storeAddress = WireDefault(false.B)

  switch(state) {
    is(s_idle) {
      eat()
      when (io.agu.fire()) {
        nextState := s_shake
      }
    }

    is(s_shake) {
      io.tlb.virtualAddress.valid := !io.flush
      io.tlb.virtualAddress.bits := lsuEntry.aguInfo.address.address

      when (io.tlb.virtualAddress.fire()) {
        setStateWhenWait()
      } otherwise {
        when (io.flush) {
          nextState := s_idle
        } otherwise {
          nextState := s_shake
        }
      }
    }

    is(s_wait) {
      setStateWhenWait()
    }

    is(s_send) {
      setStateWhenSend()
    }

    is(s_flush) {
      when (io.tlb.queryResult.valid) {
        nextState := s_idle
      } otherwise {
        nextState := s_flush
      }
    }
  }
  state := nextState

  def eat() = {
    eatNewAGU := !io.flush
  }

  def setStateWhenSend() = {
    io.res.valid := !io.flush

    when (io.flush) {
      nextState := s_idle
    } otherwise {
      when (io.res.fire()) {
        eat()
        when (io.agu.valid) {
          nextState := s_shake
        } otherwise {
          nextState := s_idle
        }
      } otherwise {
        nextState := s_send
      }
    }
  }

  def setStateWhenWait() = {
    when (io.tlb.queryResult.valid) {
      storeAddress := true.B
      setStateWhenSend()
    } otherwise {
      when (io.flush) {
        nextState := s_flush
      } otherwise {
        nextState := s_wait
      }
    }
  }

  io.agu.ready := eatNewAGU

  when (io.agu.fire) {
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

  // debug
  io.debug.state := state
  io.debug.nextState := nextState

  log("Current state: %d Next state: %d", state, nextState)
}
