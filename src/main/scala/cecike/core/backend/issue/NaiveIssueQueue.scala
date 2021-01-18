package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class NaiveIssueQueueIO(val fuNum: Int) extends Bundle {
  val microOpIn = Flipped(DecoupledIO(Vec(decodeWidth, Valid(new IssueMicroOp))))
  val readyRdMask = Input(UInt(physicalRegisterNum.W))
  val branchInfo = Input(new BranchInfo)
  val flush = Input(Bool())
  val fuTypes = Input(Vec(fuNum, UInt(FunctionUnitType.fuTypeWidth.W)))

  val microOpOut = Output(Vec(fuNum, DecoupledIO(new IssueMicroOp)))
}

class NaiveIssueQueue(fuNum: Int, depth: Int) extends Module {
  require(fuNum > 0)
  require(depth > 0)

  val io = IO(new NaiveIssueQueueIO(fuNum))

  val queueEntries = for (_ <- 0 until depth) yield {
    val entry = Module(new IssueQueueEntry);
    entry
  }
  val queueEntriesIO = VecInit(queueEntries.map(_.io))

  // Common inputs
  queueEntriesIO.foreach { p =>
    p.flush := io.flush
    p.branchInfo := io.branchInfo
    p.readyRdMask := io.readyRdMask
    p.microOpIn := DontCare
    p.select := DontCare
  }

  // Select logic
  val entryEmptyOH = Cat(queueEntriesIO.map(!_.valid).reverse)
  val entryReadyOH = Cat(queueEntriesIO.map( p => p.ready && p.valid).reverse)
  val entryMaskOH = for (i <- 0 until fuNum) yield {
    Cat(queueEntriesIO.map { p =>
      (p.microOpOut.fuType & io.fuTypes(i)).orR()
    }.reverse)
  }

  val entryEmpty = MultiBinaryPriorityEncoder(entryEmptyOH, decodeWidth)
  val entryReady = MultiBinaryPriorityEncoder(entryReadyOH, entryMaskOH, fuNum)
  io.microOpIn.ready := (!io.microOpIn.valid) || (io.microOpIn.bits zip entryEmpty._1).map { p =>
    !p._1.valid || p._2.valid
  }.reduce(_&&_)

  for (i <- 0 until fuNum) {
    val entry = entryReady._1(i)
    io.microOpOut(i).valid := entry.valid
    io.microOpOut(i).bits := queueEntriesIO(entry.bits).microOpOut
    queueEntriesIO(entry.bits).select := io.microOpOut(i).fire
  }

  for (i <- 0 until decodeWidth) {
    val entry = entryEmpty._1(i)
    queueEntriesIO(entry.bits).microOpIn.bits := io.microOpIn.bits(i).bits
    queueEntriesIO(entry.bits).microOpIn.valid := io.microOpIn.valid && io.microOpIn.bits(i).valid && entry.valid
  }
}
