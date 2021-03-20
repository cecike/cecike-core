package cecike.core.backend

import cecike.core.backend.decode.Decoder
import cecike.core.backend.execution.{CommonFunctionUnit, MemoryFunctionUnit}
import cecike.core.backend.issue.{IssueQueueDispatcher, NaiveIssueQueue, SerialIssueQueue}
import cecike.core.backend.lsu.LoadStoreUnit
import cecike.core.backend.register.{RegisterFile, RegisterFileReadPort}
import cecike.core.backend.rename.RenameStage
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.core.memory.{MemoryReadPort, MemoryWritePort}
import cecike.core.memory.tlb.TLBQueryPort
import cecike.utils._

class BackendDebugIO extends Bundle {
  val rob = Output(new ReorderBufferDebugIO)
  val register = new RegisterFileReadPort()
}

class BackendIO extends Bundle {
  val instruction = DeqIO(Vec(decodeWidth, new InstructionBundle))
  val branchInfo = Output(new BranchInfo)
  val tlbQuery = new TLBQueryPort
  val memoryRead = new MemoryReadPort
  val memoryWrite = new MemoryWritePort
  val debug = new BackendDebugIO
}

class Backend extends Module {
  val io = IO(new BackendIO)

  // Sub modules
  val decoder = Module(new Decoder)
  val rename = Module(new RenameStage)
  val dispatcher = Module(new IssueQueueDispatcher)
  val naiveIssueQueue = Module(new NaiveIssueQueue(commonIssueWidth, commonQueueDepth))
  val lsuIssueQueue = Module(new SerialIssueQueue(lsuQueueDepth))
  val mainALU = Module(new CommonFunctionUnit(true, true))
  val subALU = Module(new CommonFunctionUnit(true, false))
  val agu = Module(new MemoryFunctionUnit)
  val lsu = Module(new LoadStoreUnit)
  val register = Module(new RegisterFile(Array(true, true, true)))
  val rob = Module(new ReorderBuffer)

  def rdMask(data: Valid[UInt]): UInt ={
    (data.valid << data.bits).asUInt()
  }
  val readyRdMask = rdMask(mainALU.io.readyRd) | rdMask(subALU.io.readyRd) | rdMask(agu.io.readyRd)

  decoder.io.flush := rob.io.flush
  decoder.io.microOpIn.valid := io.instruction.valid
  io.instruction.ready := decoder.io.microOpIn.ready
  for (i <- 0 until decodeWidth) {
    decoder.io.microOpIn.bits(i) := MicroOp(io.instruction.bits(i))
  }

  rename.io.microOpIn <> decoder.io.microOpOut
  rename.io.rdCommitPort := rob.io.mapTableRdCommit
  rename.io.rdCommitDeallocateReqPort := rob.io.freelistCommitDeallocateReqPort

  rename.io.backendWritePort(0) := mainALU.io.readyRd
  rename.io.backendWritePort(1) := subALU.io.readyRd
  rename.io.backendWritePort(2) := agu.io.readyRd

  rename.io.flush := rob.io.flush

  dispatcher.io.microOpIn <> rename.io.microOpOut
  dispatcher.io.currentROBAddressBase := rob.io.currentAddressBase
  dispatcher.io.acceptFuTypes(0) := naiveIssueQueue.io.acceptFuTypes
  dispatcher.io.acceptFuTypes(1) := lsuIssueQueue.io.acceptFuTypes

  naiveIssueQueue.io.flush := rob.io.flush
  naiveIssueQueue.io.microOpIn <> dispatcher.io.microOpOut(0)
  naiveIssueQueue.io.readyRdMask := readyRdMask
  naiveIssueQueue.io.busyTable := rename.io.table
  naiveIssueQueue.io.fuTypes(0) := mainALU.io.fuType
  naiveIssueQueue.io.fuTypes(1) := subALU.io.fuType

  lsuIssueQueue.io.flush := rob.io.flush
  lsuIssueQueue.io.microOpIn <> dispatcher.io.microOpOut(1)
  lsuIssueQueue.io.readyRdMask := readyRdMask
  lsuIssueQueue.io.busyTable := rename.io.table
  lsuIssueQueue.io.fuTypes(0) := agu.io.fuType

  mainALU.io.flush := rob.io.flush
  mainALU.io.microOpIn <> naiveIssueQueue.io.microOpOut(0)
  mainALU.io.rsRead(0) <> register.io.readPort(0)
  mainALU.io.rsRead(1) <> register.io.readPort(1)
  io.branchInfo := mainALU.io.branchInfo

  subALU.io.flush := rob.io.flush
  subALU.io.microOpIn <> naiveIssueQueue.io.microOpOut(1)
  subALU.io.rsRead(0) <> register.io.readPort(2)
  subALU.io.rsRead(1) <> register.io.readPort(3)

  agu.io.flush := rob.io.flush
  agu.io.microOpIn <> lsuIssueQueue.io.microOpOut(0)
  agu.io.rsRead(0) <> register.io.readPort(4)
  agu.io.rsRead(1) <> register.io.readPort(5)

  agu.io.loadStoreInfo <> lsu.io.agu

  lsu.io.flush := rob.io.flush
  lsu.io.tlb <> io.tlbQuery
  lsu.io.memoryRead <> io.memoryRead
  lsu.io.memoryWrite <> io.memoryWrite
  lsu.io.storeCommit := rob.io.storeCommit

  register.io.writePort(0) := mainALU.io.rdWrite
  register.io.writePort(1) := subALU.io.rdWrite
  register.io.writePort(2) := agu.io.rdWrite

  rob.io.microOpIn <> dispatcher.io.robMicroOpOut
  rob.io.branchInfo := mainALU.io.branchInfo
  rob.io.robReady(0) := mainALU.io.readyROB
  rob.io.robReady(1) := subALU.io.readyROB
  rob.io.robReady(2) := agu.io.readyROB

  io.debug.rob := rob.io.debug
  io.debug.register <> register.io.debug
}
