package cecike.core.backend.decode

import cecike.CecikeModule
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class DecoderIO extends Bundle {
  val flush = Input(Bool())
  val microOpIn = DeqIO(Vec(decodeWidth, new MicroOp))
  val microOpOut = EnqIO(Vec(decodeWidth, new MicroOp))
}

class Decoder extends CecikeModule {
  val io = IO(new DecoderIO)

  val microOpRegValid = RegInit(false.B)
  val microOpReg = Reg(Vec(decodeWidth, new MicroOp))

  val decodedMicroOp = WireDefault(io.microOpIn.bits)
  for (i <- 0 until decodeWidth) {
    val cs = Rv64InstructionTable(io.microOpIn.bits(i).instruction)
    decodedMicroOp(i).controlSignal := cs
    decodedMicroOp(i).controlSignal.immediate := CS.immediate(io.microOpIn.bits(i).instruction,
      cs.instType)
  }

  io.microOpIn.ready := false.B
  when (io.flush) {
    microOpRegValid := false.B
  } otherwise {
    when (!microOpRegValid || io.microOpOut.fire()) {
      io.microOpIn.ready := true.B
      microOpReg := decodedMicroOp
      microOpRegValid := io.microOpIn.fire()
    }
  }

  io.microOpOut.bits := microOpReg
  io.microOpOut.valid := microOpRegValid && !io.flush
  log("%x", io.microOpOut.bits(0).immediate())
}
