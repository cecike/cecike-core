package cecike.core

import cecike.CecikeModule
import cecike.core.backend.Backend
import cecike.core.common.Constants._
import cecike.core.frontend.Frontend
import cecike.core.memory.tlb.TranslationLookasideBuffer
import cecike.core.memory.{MemoryReadPort, MemoryWritePort}
import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3.util._

class SimCoreIO extends Bundle {
  val iRead = new MemoryReadPort
  val dRead = new MemoryReadPort
  val dWrite = new MemoryWritePort
}

class SimCore extends CecikeModule {
  val io = IO(new SimCoreIO)
}

class SimpleSimCore extends SimCore {
  val frontend = Module(new Frontend)
  val backend = Module(new Backend)
  val dummyTLB = Module(new TranslationLookasideBuffer)

  val endOfCacheLine = frontend.io.memoryRead.addressInfo.bits.address(cacheLineAddressWidth - 1, 2).andR()
  io.iRead.addressInfo.valid := frontend.io.memoryRead.addressInfo.valid
  io.iRead.addressInfo.bits.address := frontend.io.memoryRead.addressInfo.bits.address
  io.iRead.addressInfo.bits.size := Mux(endOfCacheLine, MemorySize.word, MemorySize.double)

  io.iRead.data.ready := true.B

  frontend.io.memoryRead.data.valid := io.iRead.data.valid
  frontend.io.memoryRead.data.bits(0) := io.iRead.data.bits(instructionLen - 1, 0)
  frontend.io.memoryRead.data.bits(1) := io.iRead.data.bits(xLen - 1, instructionLen)

  frontend.io.backendPC := backend.io.pc
  frontend.io.branchInfo := backend.io.branchInfo

  backend.io.memoryRead <> io.dRead
  backend.io.memoryWrite <> io.dWrite
  backend.io.instruction <> frontend.io.instruction
  backend.io.tlbQuery <> dummyTLB.io.query

  backend.io.debug.register.addr := 0.U
}

object SimpleSimCore {
  def main(args: Array[String]): Unit = {
    (new chisel3.stage.ChiselStage).execute(
      Array("-X", "verilog"),
      Seq(ChiselGeneratorAnnotation(() => new SimpleSimCore)))
  }
}