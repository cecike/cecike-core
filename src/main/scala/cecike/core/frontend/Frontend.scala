package cecike.core.frontend

import cecike.CecikeModule
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.core.memory.InstructionMemoryReadPort
import cecike.utils._

class FrontEndIO extends Bundle {
  val instruction = EnqIO(Vec(decodeWidth, new InstructionBundle))
  val backendPC = Flipped(Valid(UInt(xLen.W)))
  val branchInfo = Input(new BranchInfo) // to update branch predictor
  val memoryRead = new InstructionMemoryReadPort
}

class Frontend extends CecikeModule {
  val io = IO(new FrontEndIO)

  val pc = RegInit(pcInitValue)
  val hasMemReq = RegInit(false.B)
  val stagedPC = Reg(UInt(xLen.W))
  val stagedBackendPC = Reg(Valid(UInt(xLen.W)))
  val stagedInstruction = Reg(Valid(Vec(decodeWidth, new InstructionBundle)))
  when (reset.asBool()) {
    stagedBackendPC.valid := false.B
    stagedInstruction.valid := false.B
  }

  val memReqPending = hasMemReq && !io.memoryRead.data.valid
  val memReqDone = hasMemReq && io.memoryRead.data.valid

  val stagedInstructionInvalid = io.backendPC.valid || stagedBackendPC.valid
  val backendNotStall = !stagedInstruction.valid || io.instruction.fire() || stagedInstructionInvalid
  val backendStall = !backendNotStall

  log("PC: %x HasMemReq: %d Bked not stall: %x", pc, hasMemReq, backendNotStall)

  val instructionBundle = Wire(Vec(decodeWidth, new InstructionBundle))
  for (i <- 0 until decodeWidth) {
    val instruction = io.memoryRead.data.bits(i)
    val bundle = instructionBundle(i)
    bundle.pc := stagedPC + (i.U << 2.U)
    bundle.valid := instruction.valid && io.memoryRead.data.valid
    bundle.instruction := instruction.bits
    bundle.branchPredictionInfo.taken := false.B
    bundle.branchPredictionInfo.dest := DontCare
  }

  def wrappedNext(data: UInt) = {
    val directNext = data + 8.U
    val crossLineNext = directNext(xLen - 1, cacheLineAddressWidth) ## 0.U(cacheLineAddressWidth.W)
    val crossCacheLine = directNext(xLen - 1, cacheLineAddressWidth) =/= data(xLen - 1, cacheLineAddressWidth)
    Mux(crossCacheLine, crossLineNext, directNext)
  }

  val realPC = Mux(stagedBackendPC.valid, stagedBackendPC.bits,
    Mux(io.backendPC.valid,
      io.backendPC.bits, pc))
  log("Real PC: %x, next: %x", realPC, wrappedNext(realPC))

  io.memoryRead.addressInfo.valid := false.B
  io.memoryRead.addressInfo.bits.address := 0.U
  io.memoryRead.addressInfo.bits.compact := false.B
  when (
    (!hasMemReq && backendNotStall) ||
      (memReqDone && io.instruction.fire())) {
    log("Issue a new req for pc %x", realPC)
    // Issue a new memory req
    io.memoryRead.addressInfo.valid := true.B
    io.memoryRead.addressInfo.bits.address := realPC
    hasMemReq := true.B

    // Clear old state
    stagedBackendPC.valid := false.B

    // Set new pc and backup old pc
    pc := wrappedNext(realPC)
    stagedPC := realPC
  } otherwise {
    when (io.backendPC.valid) {
      log("Got a backend pc change")
      stagedBackendPC := io.backendPC
    }

    // fucking backend stall
    when (memReqDone && !io.instruction.fire()) {
      log("Backend is full")
      hasMemReq := false.B
      stagedInstruction.bits := instructionBundle
      stagedInstruction.valid := true.B
    }
  }

  io.instruction.valid := memReqDone
  io.instruction.bits := instructionBundle
  when (stagedInstruction.valid) {
    log("Use staged instructions")
    io.instruction.valid := stagedInstruction.valid
    io.instruction.bits := stagedInstruction.bits
    when (io.instruction.fire()) {
      log("Staged instruction clear")
      stagedInstruction.valid := false.B
    }
  }

  log(io.memoryRead.data.valid, "Got data for %x", stagedPC)
  log(io.instruction.fire(), "Instruction at %x fired", io.instruction.bits(0).pc)
}
