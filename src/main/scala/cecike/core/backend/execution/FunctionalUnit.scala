package cecike.core.backend.execution

import cecike.core.backend.execution.raw.{RawALU, RawBRU}
import cecike.core.backend.register.{RegisterFileReadPort, RegisterFileWritePort}
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class FunctionalUnitIO(val hasBRU: Boolean) extends Bundle {
  val flush = Input(Bool())
  val microOpIn = Input(UndirectionalValid(new IssueMicroOp))
  val rsRead = Vec(2, Flipped(new RegisterFileReadPort))

  val fuType = Output(UInt(FunctionUnitType.fuTypeWidth.W))
  val readyROB = Valid(UInt(robAddressWidth.W))
  val readyRd = Valid(UInt(physicalRegisterAddressWidth.W))
  val rdWrite = Flipped(new RegisterFileWritePort)
  val branchInfo = if (hasBRU) Output(new BranchInfo) else null
}

class FunctionalUnit(hasALU: Boolean, hasBRU: Boolean) extends Module {
  val io = IO(new FunctionalUnitIO(hasBRU))

  val microOpIn = io.microOpIn

  io.fuType := FunctionUnitType.fuTypeCode(hasALU, hasBRU)
  // Stage 1 - read register and select src
  io.rsRead(0).addr := io.microOpIn.bits.rs1Info.addr
  io.rsRead(1).addr := io.microOpIn.bits.rs2Info.addr

  val src1 = RegNext(io.rsRead(0).data)
  val src2 = RegNext(Mux(InstructionType.isRegToRegInstruction(io.microOpIn.bits.instType),
    io.rsRead(1).data,
    io.microOpIn.bits.immediate))
  val stage2MicroOp = Reg(UndirectionalValid(new IssueMicroOp))
  stage2MicroOp := microOpIn

  // Stage 2 - compute

  // ALU
  val alu = Module(new RawALU)
  alu.io.op := stage2MicroOp.bits.fuOp
  alu.io.src1 := src1
  alu.io.src2 := src2
  val aluResult = alu.io.result

  // BRU
  val bru = Module(new RawBRU)
  bru.io.op := stage2MicroOp.bits.fuOp
  bru.io.pc := stage2MicroOp.bits.pc
  bru.io.offset := stage2MicroOp.bits.immediate
  bru.io.src1 := src1
  bru.io.src2 := src2
  val mispredictedTaken = bru.io.resultPC.valid =/= stage2MicroOp.bits.branchPredictionInfo.taken
  val mispredictedDest = bru.io.resultPC.bits =/= stage2MicroOp.bits.branchPredictionInfo.dest
  if (hasBRU) {
    io.branchInfo.mispredicted := mispredictedTaken || mispredictedDest
    io.branchInfo.taken := bru.io.resultPC.valid
    io.branchInfo.dest := stage2MicroOp.bits.branchPredictionInfo.dest
  }

  // Output
  io.rdWrite.valid := stage2MicroOp.valid && stage2MicroOp.bits.rdInfo.valid
  io.rdWrite.data := Mux(hasBRU.B && bru.io.result.valid, bru.io.result.bits, aluResult)
  io.rdWrite.addr := stage2MicroOp.bits.rdInfo.bits
  // TODO: For now all FU is bypassed, modify here in the future
  io.readyROB.valid := stage2MicroOp.valid
  io.readyROB.bits := stage2MicroOp.bits.robIndex
  io.readyRd.valid := io.microOpIn.valid && io.microOpIn.bits.rdInfo.valid
  io.readyRd.bits := io.microOpIn.bits.rdInfo.bits
}
