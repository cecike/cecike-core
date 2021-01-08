package cecike.core.backend.rename

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class MapTableReadPort extends Bundle {
  val logicalAddr = Input(UInt(logicalRegisterAddressWidth.W))
  val physicalAddr = Output(UInt(physicalRegisterAddressWidth.W))
}

class MapTableWritePort extends Bundle {
  val valid = Input(Bool())
  val logicalAddr = Input(UInt(logicalRegisterAddressWidth.W))
  val physicalAddr = Input(UInt(physicalRegisterAddressWidth.W))
}

class MapTableIO extends Bundle {
  val rs1ReadPort = Vec(decodeWidth, new MapTableReadPort)
  val rs2ReadPort = Vec(decodeWidth, new MapTableReadPort)
  val rdReadPort = Vec(decodeWidth, new MapTableReadPort)
  val rdWritePort = Vec(decodeWidth, new MapTableWritePort)
  val rdCommitPort = Vec(decodeWidth, new MapTableWritePort)
  val flush = Input(Bool())
}

class MapTable extends Module {
  val io = IO(new MapTableIO)

  val currentMapTable = Mem(logicalRegisterNum, UInt(physicalRegisterAddressWidth.W))
  val architecturalMapTable = Mem(logicalRegisterNum, UInt(physicalRegisterAddressWidth.W))

  io.rs1ReadPort.foreach(p => p.physicalAddr := Mux(p.logicalAddr.orR(), currentMapTable(p.logicalAddr), 0.U))
  io.rs2ReadPort.foreach(p => p.physicalAddr := Mux(p.logicalAddr.orR(), currentMapTable(p.logicalAddr), 0.U))
  io.rdReadPort.foreach(p => p.physicalAddr := Mux(p.logicalAddr.orR(), currentMapTable(p.logicalAddr), 0.U))

  io.rdCommitPort.foreach(p => when (p.valid) {architecturalMapTable.write(p.logicalAddr, p.physicalAddr)})

  val flush = RegNext(io.flush)
  when (flush) {
    for (i <- 0 until logicalRegisterNum) {
      currentMapTable.write(i.U, architecturalMapTable(i.U))
    }
  } otherwise {
    io.rdWritePort.foreach(p => when (p.valid) {currentMapTable.write(p.logicalAddr, p.physicalAddr)})
  }
}
