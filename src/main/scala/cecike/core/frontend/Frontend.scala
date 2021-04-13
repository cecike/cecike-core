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

  val s_idle :: s_wait :: s_wait_kill :: s_wait_send :: Nil = Enum(4)
  val state = RegInit(s_idle)
  val nextState = Wire(UInt(2.W))
  nextState := s_idle

  def instructionBundle(i: Int) = {
      val instruction = io.memoryRead.data.bits(i)
      val bundle = Wire(new InstructionBundle)
      val pc: UInt = stagedPC + (i.U << 2.U)
      bundle.pc := pc
      bundle.valid := (stagedPC(xLen - 1, cacheLineAddressWidth) === pc(xLen - 1, cacheLineAddressWidth))
      bundle.instruction := instruction
      bundle.branchPredictionInfo.taken := false.B
      bundle.branchPredictionInfo.dest := DontCare
      bundle
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
  io.instruction.valid := false.B
  io.instruction.bits := DontCare

  switch(state) {
    is(s_idle) {
      issueNewReq()
      nextState := s_wait
    }

    is(s_wait) {
      when (io.memoryRead.data.valid && io.backendPC.valid) {
        //log("1")
        issueNewReq()
        nextState := s_wait
      } .elsewhen(io.memoryRead.data.valid) {
        //log("2")
        io.instruction.valid := true.B
        io.instruction.bits := (0 until decodeWidth).map(instructionBundle)
        when (io.instruction.fire()) {
          issueNewReq()
          nextState := s_wait
        } otherwise {
          //log("3")
          stagedInstruction.valid := true.B
          stagedInstruction.bits := (0 until decodeWidth).map(instructionBundle)
          nextState := s_wait_send
        }
      } .elsewhen(io.backendPC.valid) {
        //log("4")
        keepReq()
        stagedBackendPC := io.backendPC
        nextState := s_wait_kill
      } .otherwise {
        //log("5")
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
        stagedBackendPC.valid := false.B
        issueNewReq()
        nextState := s_wait
      } otherwise {
        io.instruction.valid := true.B
        io.instruction.bits := stagedInstruction.bits
        when (io.instruction.fire()) {
          stagedInstruction.valid := false.B
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
    //log("Issue a new req for %x", realPC)
    io.memoryRead.addressInfo.valid := true.B
    io.memoryRead.addressInfo.bits.address := realPC
    io.memoryRead.addressInfo.bits.compact := false.B

    pc := wrappedNext(realPC)
    stagedPC := realPC
    stagedBackendPC.valid := false.B
  }

  def keepReq() = {
    io.memoryRead.addressInfo.valid := true.B
    io.memoryRead.addressInfo.bits.address := stagedPC
    io.memoryRead.addressInfo.bits.compact := false.B
  }

  log(io.memoryRead.data.valid, "Got data for %x", stagedPC)
  log(p"$state $nextState")
  log(io.instruction.fire(), "Instruction at %x fired %x - %b %x - %b",
    io.instruction.bits(0).pc,
    io.instruction.bits(0).instruction,
    io.instruction.bits(0).valid,
    io.instruction.bits(1).instruction,
    io.instruction.bits(1).valid)
}
