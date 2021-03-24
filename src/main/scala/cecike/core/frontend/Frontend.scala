package cecike.core.frontend

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
import cecike.core.memory.{InstructionMemoryReadPort, MemoryReadPort, MemoryWritePort}
import cecike.core.memory.tlb.TLBQueryPort
import cecike.utils._

class FrontEndIO extends Bundle {
  val instruction = EnqIO(Vec(decodeWidth, new InstructionBundle))
  val branchInfo = Input(new BranchInfo) // to update branch predictor
  val memoryRead = new InstructionMemoryReadPort
  val flush = Input(Bool())
}

class Frontend extends Module {
  val io = IO(new FrontEndIO)

  val pc = RegInit(pcInitValue)

  val directNextPC = pc + 8.U
  val crossLineNextPC = directNextPC(xLen, cacheLineAddressWidth) ## 0.U(cacheLineAddressWidth.W)
  val crossCacheLine = directNextPC(xLen, cacheLineAddressWidth) === pc(xLen, cacheLineAddressWidth)
  val normalNextPC = Mux(crossCacheLine, crossLineNextPC, directNextPC)


}
