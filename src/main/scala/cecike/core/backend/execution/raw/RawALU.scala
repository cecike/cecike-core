package cecike.core.backend.execution.raw

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils._

class RawALUIO extends Bundle {
  val op = Input(UInt(ALUOp.aluOpWidth.W))
  val pc = Input(UInt(xLen.W))
  val src1 = Input(UInt(xLen.W))
  val src2 = Input(UInt(xLen.W))
  val result = Output(UInt(xLen.W))
}

class RawALU extends Module {
  val io = IO(new RawALUIO)

  val op = UIntToOH(io.op)
  val src1 = io.src1
  val src2 = io.src2

  val src1W = src1(wLen - 1, 0)
  val src2W = src2(wLen - 1, 0)

  val resultTable = Array(
    op(ALUOp.ADD) -> (src1 + src2),
    op(ALUOp.SUB) -> (src1 - src2),
    op(ALUOp.SLT) -> Mux(src1.asSInt < src2.asSInt, 1.U, 0.U),
    op(ALUOp.SLTU) -> Mux(src1 < src2, 1.U, 0.U),
    op(ALUOp.AND) -> (src1 & src2),
    op(ALUOp.OR) -> (src1 | src2),
    op(ALUOp.XOR) -> (src1 ^ src2),
    op(ALUOp.SLL) -> (src1 << src2(5, 0))(xLen - 1, 0).asUInt,
    op(ALUOp.SRL) -> (src1 >> src2(5, 0))(xLen - 1, 0).asUInt,
    op(ALUOp.SRA) -> (src1.asSInt >> src2(5, 0))(xLen - 1, 0).asUInt,
    op(ALUOp.LUI) -> src2,
    op(ALUOp.AUIPC) -> (io.pc + src2),
    op(ALUOp.ADDW) -> SignExtension(src1W + src2W),
    op(ALUOp.SUBW) -> SignExtension(src1W - src2W),
    op(ALUOp.SLLW) -> SignExtension((src1 << src2(4, 0))(wLen - 1, 0)),
    op(ALUOp.SRLW) -> SignExtension((src1 >> src2(4, 0))(wLen - 1, 0)),
    op(ALUOp.SRAW) -> SignExtension((src1.asSInt >> src2(4, 0))(wLen - 1, 0))
  )

  io.result := Mux1H(resultTable)
}
