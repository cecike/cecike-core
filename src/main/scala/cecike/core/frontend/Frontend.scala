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

// States -
// idle: Issue a new memreq forever
// wait: wait for mem resp
// wait_kill: wait to kill current mem req
// wait_send: wait to send result to backend
class Frontend extends CecikeModule {
  override val hasLog = true.B
  val io = IO(new FrontEndIO)

  val pc = RegInit(pcInitValue)
  val hasMemReq = RegInit(false.B)
  val stagedPC = Reg(UInt(xLen.W))
  val stagedBackendPC = Reg(UInt(xLen.W))
  val stagedBackendPCValid = RegInit(false.B)
  val stagedInstruction = Reg(Vec(decodeWidth, UInt(xLen.W)))
  val stagedInstructionValid = RegInit(false.B)

  val notStageBackend = WireDefault(false.B)

  val s_idle :: s_wait :: s_wait_kill :: s_wait_send :: Nil = Enum(4)
  val state = RegInit(s_idle)
  val nextState = WireDefault(s_idle)
  nextState := s_idle

  def cacheLineEqual(a: UInt, b: UInt) = {
    a(xLen - 1, cacheLineAddressWidth) === b(xLen - 1, cacheLineAddressWidth)
  }

  def instructionBundle(i: Int, source: Vec[UInt]) = {
      val instruction = source(i)
      val bundle = Wire(new InstructionBundle)
      val pc: UInt = stagedPC + (i.U << 2.U)
      bundle.pc := pc
      bundle.valid := cacheLineEqual(pc, stagedPC)
      bundle.instruction := instruction
      bundle.branchPredictionInfo.taken := false.B
      bundle.branchPredictionInfo.dest := DontCare
      bundle
  }

  def wrappedNext(data: UInt) = {
    val directNext = data + 8.U
    val crossLineNext = directNext(xLen - 1, cacheLineAddressWidth) ## 0.U(cacheLineAddressWidth.W)
    Mux(cacheLineEqual(directNext, data), directNext, crossLineNext)
  }

  def realPC = Mux(stagedBackendPCValid, stagedBackendPC,
    Mux(io.backendPC.valid,
      io.backendPC.bits, pc))
  log("Real PC: %x, next: %x", realPC, wrappedNext(realPC))

  io.memoryRead.addressInfo.valid := false.B
  io.memoryRead.addressInfo.bits.address := 0.U
  io.memoryRead.addressInfo.bits.compact := false.B
  io.instruction.valid := false.B
  for (i <- 0 until decodeWidth) {
    io.instruction.bits(i) := instructionBundle(i, io.memoryRead.data.bits)
  }

  when (io.backendPC.valid && !notStageBackend) {
    stagedBackendPCValid := true.B
    stagedBackendPC := io.backendPC.bits
  }

  when (io.memoryRead.data.valid) {
    stagedInstruction := io.memoryRead.data.bits
  }

  switch(state) {
    is(s_idle) {
      issueNewReq()
      nextState := s_wait
    }

    is(s_wait) {
      val memOk = io.memoryRead.data.valid
      when (io.backendPC.valid) {
        nextState := Mux(memOk, s_idle, s_wait_kill)
      } .elsewhen(memOk) {
        io.instruction.valid := true.B
        when (io.instruction.ready) {
          issueNewReq()
          nextState := s_wait
        } otherwise {
          stagedInstructionValid := true.B
          nextState := s_wait_send
        }
      } .otherwise {
        keepReq()
        nextState := s_wait
      }
    }

    is(s_wait_kill) {
      keepReq()
      when (io.memoryRead.data.valid) {
        nextState := s_idle
      } otherwise {
        nextState := s_wait_kill
      }
    }

    is(s_wait_send) {
      when (io.backendPC.valid) {
        issueNewReq()
        nextState := s_wait
      } otherwise {
        io.instruction.valid := true.B
        io.instruction.bits := (0 until decodeWidth).map(instructionBundle(_, stagedInstruction))
        when (io.instruction.fire()) {
          stagedInstructionValid := false.B
          issueNewReq()
          nextState := s_wait
        } otherwise {
          nextState := s_wait_send
        }
      }
    }
  }

  state := nextState

  def issueNewReq(): Unit = {
    log("Issue a new req for %x", realPC)
    io.memoryRead.addressInfo.valid := true.B
    io.memoryRead.addressInfo.bits.address := realPC
    io.memoryRead.addressInfo.bits.compact := false.B

    pc := wrappedNext(realPC)
    stagedPC := realPC
    notStageBackend := true.B
    stagedBackendPCValid := false.B
  }

  def keepReq() = {
    io.memoryRead.addressInfo.valid := true.B
    io.memoryRead.addressInfo.bits.address := stagedPC
    io.memoryRead.addressInfo.bits.compact := false.B
  }

  log(io.memoryRead.data.valid, "Got data for %x", stagedPC)
  log(io.memoryRead.data.valid, p"${io.memoryRead.data.bits}")
  log(p"$state $nextState")
  log(io.instruction.fire(), "Instruction at %x fired %x - %b %x - %b",
    io.instruction.bits(0).pc,
    io.instruction.bits(0).instruction,
    io.instruction.bits(0).valid,
    io.instruction.bits(1).instruction,
    io.instruction.bits(1).valid)
}
