package cecike.core.backend.decode

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.{CS, MicroOp}
import cecike.utils._

class DecoderIO extends Bundle {
  val microOpIn = Flipped(DecoupledIO(Vec(decodeWidth, new MicroOp)))
  val microOpOut = DecoupledIO(Vec(decodeWidth, new MicroOp))
}

class Decoder extends Module {
  val io = IO(new DecoderIO)

  io.microOpIn.ready := io.microOpOut.ready
  io.microOpOut.valid := io.microOpIn.valid

  for (i <- 0 until decodeWidth) {
    io.microOpOut.bits(i) := io.microOpIn.bits(i)
    io.microOpOut.bits(i).controlSignal := Rv64InstructionTable(io.microOpIn.bits(i).instruction)
    io.microOpOut.bits(i).controlSignal.immediate := CS.immediate(io.microOpIn.bits(i).instruction,
      io.microOpOut.bits(i).controlSignal.instType)
  }
}
