package cecike.core.backend.execution

import cecike.core.backend.execution.raw.{RawALU, RawBRU}
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class CommonFunctionUnit(hasALU: Boolean, hasBRU: Boolean) extends
  FunctionUnit(hasALU, hasBRU, false, false) {
  val microOpIn = io.microOpIn

  io.fuType := FunctionUnitType.fuTypeCode(hasALU, hasBRU)
  // Stage 1 - read register and select src
  io.rsRead(0).addr := io.microOpIn.bits.rs1Info.addr
  io.rsRead(1).addr := io.microOpIn.bits.rs2Info.addr

  val rsData0 = io.rsRead(0).data
  val rsData1 = io.rsRead(1).data
  val src1 = RegNext(rsData0)
  val src2 = RegNext(Mux(InstructionType.needRs2Instruction(io.microOpIn.bits.instType),
    rsData1,
    io.microOpIn.bits.immediate))
  val stage2MicroOp = Reg(UndirectionalValid(new IssueMicroOp))

  when (reset.asBool || io.flush) {
    stage2MicroOp.valid := false.B
  } otherwise {
    stage2MicroOp := microOpIn
  }

  val op = stage2MicroOp.bits
  val opValid = stage2MicroOp.valid

  // Stage 2 - compute

  // ALU
  val isALUOp = hasALU.B && FunctionUnitType.typeMatch(op.fuType, FunctionUnitType.FU_ALU)
  val alu = Module(new RawALU)
  alu.io.op := op.fuOp
  alu.io.pc := op.pc
  alu.io.src1 := src1
  alu.io.src2 := src2
  val aluResult = alu.io.result

  // BRU
  val isBRUOp = hasBRU.B && FunctionUnitType.typeMatch(op.fuType, FunctionUnitType.FU_BRU)
  val bru = Module(new RawBRU)
  bru.io.valid := isBRUOp
  bru.io.op := op.fuOp
  bru.io.pc := op.pc
  bru.io.offset := op.immediate
  bru.io.src1 := src1
  bru.io.src2 := src2
  val bruResult = bru.io.result.bits
  val mispredictedTaken = bru.io.resultPC.valid =/= op.branchPredictionInfo.taken
  val mispredictedDest = bru.io.resultPC.bits =/= op.branchPredictionInfo.dest
  if (hasBRU) {
    io.branchInfo.mispredicted := mispredictedTaken || mispredictedDest
    io.branchInfo.taken := bru.io.resultPC.valid
    io.branchInfo.dest := op.branchPredictionInfo.dest
    io.branchInfo.valid := isBRUOp
    io.branchInfo.robIndex := op.robIndex
  }

  val rdWriteDataTable = Array(
    isALUOp -> aluResult,
    isBRUOp -> bruResult
  )
  val rdWriteData = Mux1H(rdWriteDataTable)
  // Output
  io.microOpIn.ready := true.B

  io.rdWrite.valid := opValid && op.rdInfo.valid
  io.rdWrite.data := rdWriteData
  io.rdWrite.addr := op.rdInfo.bits

  // TODO: For now all FU is bypassed, modify here in the future
  io.readyROB.valid := opValid
  io.readyROB.bits := op.robIndex

  io.readyRd.valid := io.microOpIn.valid && io.microOpIn.bits.rdInfo.valid
  io.readyRd.bits := io.microOpIn.bits.rdInfo.bits
}
