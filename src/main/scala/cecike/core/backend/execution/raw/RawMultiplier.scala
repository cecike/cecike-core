package cecike.core.backend.execution.raw

import cecike.CecikeModule
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class RawMultiplierIn extends Bundle {
  val op = UInt(MDUOp.mduOpWidth.W)
  val pc = UInt(xLen.W)
  val src1 = UInt(xLen.W)
  val src2 = UInt(xLen.W)
}

class RawMultiplierIO extends Bundle {
  val op = Flipped(Valid(new RawMultiplierIn))
  val result = Output(Valid(UInt(xLen.W)))
}

class RawMultiplier(latency: Int = 4) extends Module {
  val io = IO(new RawMultiplierIO)

  val op = Pipe(io.op)

  val resultHi = op.bits.op(1, 0).orR()
  val leftSigned = op.bits.op =/= MDUOp.MULHU
  val rightSigned = op.bits.op =/= MDUOp.MULHU && op.bits.op =/= MDUOp.MULHSU
  val use32Bit = op.bits.op === MDUOp.MULW

  def toSigned(signed: Bool, data: UInt) = {
    Cat(signed && data(xLen - 1), data).asSInt
  }
  val src1 = toSigned(leftSigned, op.bits.src1)
  val src2 = toSigned(leftSigned, op.bits.src1)
  val product = src1 * src2
  val productLo = product(xLen - 1, 0)
  val productHi = product((xLen << 1) - 1, 0)
  val productW = SignExtension(product((xLen >> 1) - 1, 0))
  val result = Mux(resultHi, productHi,
    Mux(use32Bit, productW, productLo))

  val output = Pipe(op.valid, result, latency - 1)
  io.result.valid := output.valid
  io.result.bits := output.bits
}
