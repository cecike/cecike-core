package cecike.core.backend.register

import cecike.CecikeModule
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class RegisterFileReadPort extends Bundle {
  val addr = Input(UInt(physicalRegisterAddressWidth.W))
  val data = Output(UInt(xLen.W))
}

class RegisterFileWritePort extends Bundle {
  val valid = Input(Bool())
  val addr = Input(UInt(physicalRegisterAddressWidth.W))
  val data = Input(UInt(xLen.W))
}

class RegisterFileIO extends Bundle {
  val readPort = Vec(2 * issueWidth, new RegisterFileReadPort)
  val writePort = Vec(issueWidth, new RegisterFileWritePort)
  val debug = new RegisterFileReadPort()
}

class RegisterFile(bypass: Seq[Boolean]) extends CecikeModule {
  require(bypass.length == issueWidth)
  require(bypass.contains(true))
  val io = IO(new RegisterFileIO)

  val bypassedWritePort = (bypass zip io.writePort).filter(_._1).map(_._2)

  val registerFile = Mem(physicalRegisterNum, UInt(xLen.W))

  // Write
  io.writePort.foreach { p =>
    when(p.valid) {
      registerFile.write(p.addr, p.data)
      log("Write reg[%d] = %x", p.addr, p.data)
    }
  }

  // Read
  io.readPort.foreach { p =>
    val addr = p.addr
    val addrNotZero = p.addr.orR()
    val data = Mux(addrNotZero, registerFile.read(addr), 0.U)
    val bypassedResults = bypassedWritePort.map { q =>
      val valid = q.valid && q.addr === p.addr && addrNotZero
      val data = q.data
      (valid, data)
    }
    val result = MuxCase(data, bypassedResults)

    when (bypassedResults.map(_._1).reduce(_||_)) {
      log("Bypass read reg[%d] = %x", p.addr, result)
    } otherwise when (p.addr =/= 0.U){
      log("Normal read reg[%d] = %x", p.addr, result)
    }

    p.data := result
  }

  io.debug.data := registerFile.read(io.debug.addr)
}
