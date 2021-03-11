package cecike.core.backend.register

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

class RegisterFile(bypass: Seq[Boolean]) extends Module {
  require(bypass.length == issueWidth)
  require(bypass.contains(true))
  val io = IO(new RegisterFileIO)

  val bypassedWritePort = (bypass zip io.writePort).filter(_._1).map(_._2)

  val registerFile = Mem(physicalRegisterNum, UInt(xLen.W))

  // Write
  io.writePort.foreach { p =>
    when(p.valid) {
      registerFile.write(p.addr, p.data)
    }
  }

  // Read
  io.readPort.foreach { p =>
    val addr = p.addr
    val addrNotZero = p.addr.orR()
    val data = Mux(addrNotZero, registerFile.read(addr), 0.U)
    val bypassedResults = bypassedWritePort.map { p =>
      val valid = p.valid && p.addr === addr && addrNotZero
      val data = p.data
      (valid, data)
    }
    val result = MuxCase(data, bypassedResults)
    p.data := result
  }

  io.debug.data := registerFile.read(io.debug.addr)
}
