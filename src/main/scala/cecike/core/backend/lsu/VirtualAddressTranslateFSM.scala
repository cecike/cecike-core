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
  val storeAddressCollision = Input(Bool())
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
  io.agu.ready := false.B

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

      log("Shake %x of %x",
        lsuEntry.aguInfo.address.address,
        lsuEntry.aguInfo.opInfo.pc)

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
    log("Take new agu info of %x", io.agu.bits.opInfo.pc)
    eatNewAGU := !io.flush
    io.agu.ready := !io.flush
  }

  def setStateWhenSend() = {
    io.res.valid := !io.flush && (lsuEntry.aguInfo.load || !io.storeAddressCollision)
    log(p"${lsuEntry.aguInfo.load} ${io.storeAddressCollision}")

    log("Send %x of %x",
      lsuEntry.aguInfo.address.address,
      lsuEntry.aguInfo.opInfo.pc)

    when (io.flush) {
      nextState := s_idle
    } otherwise {
      when (io.res.fire()) {
        log("Send ok")
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

  when (io.agu.fire) {
    lsuEntry.aguInfo := io.agu.bits
    lsuEntry.status.exception := false.B
  } otherwise when (storeAddress) {
    lsuEntry.aguInfo.address.address := io.tlb.queryResult.bits.physicalAddress
    lsuEntry.status.exception := !io.tlb.queryResult.bits.valid
  }

  // Output
  val result = io.res.bits
  result := lsuEntry
  when (storeAddress) {
    result.aguInfo.address.address := io.tlb.queryResult.bits.physicalAddress
    result.status.exception := !io.tlb.queryResult.bits.valid
  }

  // debug
  io.debug.state := state
  io.debug.nextState := nextState

  log("Current state: %d Next state: %d", state, nextState)
}
