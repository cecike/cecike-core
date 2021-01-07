package cecike.core.backend.rename

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class BusyTableReadPort extends Bundle {
  val addr = Input(UInt(physicalRegisterAddressWidth.W))
  val busy = Output(Bool())
}

class BusyTableIO extends Bundle {
  val rs1ReadPort = Vec(decodeWidth, new BusyTableReadPort)
  val rs2ReadPort = Vec(decodeWidth, new BusyTableReadPort)
  val rdWritePort = Input(Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W))))
  val backendWritePort = Input(Vec(issueWidth, Valid(UInt(physicalRegisterAddressWidth.W))))
  val flush = Input(Bool())
}

class BusyTable extends Module {
  val io = IO(new BusyTableIO)

  val busyTable = RegInit(0.U(physicalRegisterNum.W))

  val rdWriteMask = io.rdWritePort
    .map(p => Mux(p.valid, (1.U(physicalRegisterNum.W) << p.bits).asUInt, 0.U))
    .reduce(_|_)

  val backendWriteMask = (~io.backendWritePort
    .map(p => Mux(p.valid, (1.U(physicalRegisterNum.W) << p.bits).asUInt, 0.U))
    .reduce(_|_)).asUInt
  val commonMask = (~(1.U(physicalRegisterNum.W))).asUInt

  val busyTableWithBackendFeedback = busyTable & backendWriteMask & commonMask

  for(i <- 0 until decodeWidth) {
    io.rs1ReadPort(i).busy := busyTableWithBackendFeedback(io.rs1ReadPort(i).addr) && !io.flush
    io.rs2ReadPort(i).busy := busyTableWithBackendFeedback(io.rs2ReadPort(i).addr) && !io.flush
  }

  busyTable := Mux(io.flush, 0.U, busyTableWithBackendFeedback | rdWriteMask)
}
