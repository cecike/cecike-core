package cecike.core.backend.execution.raw

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class RawBRUIO extends Bundle {
  val valid = Input(Bool())
  val op = Input(UInt(BRUOp.bruOpWidth.W))
  val pc = Input(UInt(xLen.W))
  val offset = Input(UInt(xLen.W))
  val src1 = Input(UInt(xLen.W))
  val src2 = Input(UInt(xLen.W))
  val result = Output(Valid(UInt(xLen.W)))
  val resultPC = Output(Valid(UInt(xLen.W)))
}

// TODO: Support `C` extension
class RawBRU extends Module {
  val io = IO(new RawBRUIO)

  val op = Mux(io.valid, UIntToOH(io.op), OHToUInt(BRUOp.BX))
  val src1 = io.src1
  val src2 = io.src2

  val branchPC = io.pc + (io.offset << 1)
  io.resultPC.bits := Mux(op(BRUOp.J), src1, branchPC)

  io.result.valid := op(BRUOp.J)
  io.result.bits := io.pc + 4.U

  val eq = src1 === src2
  val ne = !eq
  val lt = src1.asSInt < src2.asSInt
  val ge = !lt
  val ltu = src1 < src2
  val geu = !ltu

  val pcValidTable = Array(
    op(BRUOp.BX) -> false.B,
    op(BRUOp.J) -> true.B,
    op(BRUOp.EQ) -> eq,
    op(BRUOp.NE) -> ne,
    op(BRUOp.LT) -> lt,
    op(BRUOp.GE) -> ge,
    op(BRUOp.LTU) -> ltu,
    op(BRUOp.GEU) -> geu
  )

  io.resultPC.valid := Mux1H(pcValidTable)
}
