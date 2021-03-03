package cecike.core.backend

import cecike.core.backend.rename.MapTableWritePort
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class ReorderBufferEntry extends Bundle {
  // PC of the first instruction
  val basePC = UInt(xLen.W)

  val microOp = Vec(decodeWidth, new ROBMicroOp)

  def pc(n: UInt = 0.U): UInt = basePC + (n << 2.U)
}

class ReorderBufferDebugIO extends Bundle {
  val bufferFlushed = UInt(robRowNum.W)
  val bufferHead = UInt(robRowAddressWidth.W)
  val bufferTail = UInt(robRowAddressWidth.W)
  val currentEntry = new ReorderBufferEntry
  val shouldCommit = Bool()
  val commitReady = UInt(decodeWidth.W)
  val commitDone = UInt(decodeWidth.W)
}

class ReorderBufferIO extends Bundle {
  // From rename stage - store these op into ROB
  val microOpIn = Flipped(DecoupledIO(Vec(decodeWidth, new MicroOp)))
  // From FU - rob sub-entry ready info
  val robReady = Vec(issueWidth, Flipped(Valid(UInt(robAddressWidth.W))))
  // From BRU - branch status
  val branchInfo = Input(new BranchInfo)

  // From/To LSU - store commit
  val storeCommitValid = Output(Bool())
  val storeCommitReady = Input(Bool())

  val flush = Output(Bool())
  val mapTableRdCommit = Vec(decodeWidth, Flipped(new MapTableWritePort))
  val freelistCommitDeallocateReqPort = Vec(decodeWidth, Valid(UInt(physicalRegisterAddressWidth.W)))

  val debug = Output(new ReorderBufferDebugIO)
}

class ReorderBuffer extends Module {
  require(isPow2(robRowNum))
  val io = IO(new ReorderBufferIO)

  val bufferFlushed = RegInit(0.U(robRowNum.W))

  val buffer = Mem(robRowNum, new ReorderBufferEntry)
  val bufferManager = Module(new RingBufferManager(robRowNum, 1, 1))

  val bufferHead = bufferManager.io.head
  val bufferTail = bufferManager.io.tail

  def bufferEmpty(): Bool = bufferManager.io.empty
  def bufferFull(): Bool = bufferManager.io.full
  def currentEntry() = buffer(bufferHead)

  def nonEmpty[T <: Data](data: T, alt: T): T = {
    Mux(bufferEmpty(), alt, data)
  }

  def rowAddress(addr: UInt) = {
    require(addr.getWidth == robAddressWidth)
    addr(robAddressWidth - 1, robBankAddressWidth)
  }

  def bankAddress(addr: UInt) = {
    require(addr.getWidth == robAddressWidth)
    addr(robBankAddressWidth - 1, 0)
  }

  val currentEntryReady = io.robReady.map { p =>
    ((p.valid && rowAddress(p.bits) === bufferHead) << bankAddress(p.bits))(robBankNum - 1, 0).asUInt
  }.reduce(_|_)
  val currentEntryDone = Cat(currentEntry().microOp.map { p =>
    !p.valid || p.done
  }.reverse)
  val readyToCommit = (currentEntryDone | currentEntryReady).andR()
  val commit = nonEmpty(readyToCommit, false.B)

  bufferManager.io.deallocate(0) := commit

  val incomeFlush = (io.branchInfo.valid &&
    rowAddress(io.branchInfo.robIndex) === bufferHead) << bankAddress(io.branchInfo.robIndex)

  val flushMask = Wire(Vec(decodeWidth, Bool()))
  flushMask(0) := currentEntry().microOp(0).needFlush() || incomeFlush(0)
  for (i <- 1 until decodeWidth) {
    flushMask(i) := flushMask(i - 1) || currentEntry().microOp(i).needFlush() || incomeFlush(i)
  }
  val needFlush = flushMask(decodeWidth - 1)
  io.flush := needFlush
  bufferManager.io.clear := needFlush

  // store valid
  val previousMicroOpDone = Wire(Vec(decodeWidth, Bool()))
  previousMicroOpDone(0) := true.B
  for (i <- 1 until decodeWidth) {
    previousMicroOpDone(i) := previousMicroOpDone(i - 1) && currentEntry().microOp(i).done
  }

  val storeCommitValid = Wire(Vec(decodeWidth, Bool()))
  for (i <- 0 until decodeWidth) {
    val mop = currentEntry().microOp(i)
    storeCommitValid(i) := mop.valid &&
      !mop.done && mop.isStoreOp &&
      !flushMask(i) && previousMicroOpDone(i)
  }
  io.storeCommitValid := storeCommitValid.reduce(_||_) && io.storeCommitReady

  // Store branch info
  when (io.branchInfo.valid) {
    val addr = io.branchInfo.robIndex
    val row = rowAddress(addr)
    val bank = bankAddress(addr)
    buffer(row).microOp(bank).branchInfo.mispredicted := io.branchInfo.mispredicted
  }

  // Store micro op
  bufferManager.io.req.valid := true.B
  bufferManager.io.req.bits(0) := io.microOpIn.fire()
  io.microOpIn.ready := !bufferFull()
  when(io.microOpIn.fire()) {
    bufferFlushed := Mux(needFlush, (~0.U).asUInt, bufferFlushed & (~UIntToOH(bufferTail)).asUInt)
    buffer(bufferTail).microOp := VecInit(io.microOpIn.bits.map(ROBMicroOp(_)))
    buffer(bufferTail).basePC := io.microOpIn.bits(0).pc
  } otherwise {
    when (needFlush) {
      bufferFlushed := (~0.U).asUInt
    }
  }

  // Write ready info
  io.robReady.foreach { p =>
    val row = rowAddress(p.bits)
    val bank = bankAddress(p.bits)
    when (p.valid) {
      buffer(row).microOp(bank).done := true.B
    }
  }

  // Deallocate free list
  for (i <- 0 until decodeWidth) {
    io.freelistCommitDeallocateReqPort(i).valid := commit && currentEntry().microOp(i).rdValid
    io.freelistCommitDeallocateReqPort(i).bits := Mux(flushMask(i),
      currentEntry().microOp(i).physicalRd,
      currentEntry().microOp(i).oldPhysicalRd
    )
  }

  // Commit to map table
  for (i <- 0 until decodeWidth) {
    io.mapTableRdCommit(i).valid := commit &&
      !flushMask(i) &&
      currentEntry().microOp(i).rdValid &&
      currentEntry().microOp(i).orderInfo.validWithMask((~Cat(flushMask.reverse)).asUInt, i)
    io.mapTableRdCommit(i).logicalAddr := currentEntry().microOp(i).logicalRd
    io.mapTableRdCommit(i).physicalAddr := currentEntry().microOp(i).physicalRd
  }

  // Debug signals
  io.debug.bufferFlushed := bufferFlushed
  io.debug.bufferHead := bufferHead
  io.debug.bufferTail := bufferTail
  io.debug.currentEntry := currentEntry()
  io.debug.shouldCommit := commit
  io.debug.commitReady := currentEntryReady
  io.debug.commitDone := currentEntryDone
}
