package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.IssueMicroOp
import cecike.utils._

class NaiveIssueQueue(fuNum: Int, depth: Int) extends IssueQueueWithCommonEntry(fuNum, depth) {
  require(fuNum > 0)
  require(depth > 0)

  // Pipeline buffer
  val microOpOutReg = Reg(Vec(fuNum, Valid(new IssueMicroOp)))

  for (i <- 0 until fuNum) {
    io.microOpOut(i).valid := microOpOutReg(i).valid && !io.flush
    io.microOpOut(i).bits := microOpOutReg(i).bits
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
  io.microOpIn.ready := (io.microOpIn.bits zip entryEmpty._1).map { p =>
    !p._1.valid || p._2.valid
  }.reduce(_&&_)

  for (i <- 0 until fuNum) {
    val entry = entryReady._1(i)

    when (!io.flush && (io.microOpOut(i).fire() || !io.microOpOut(i).valid)) {
      microOpOutReg(i).valid := entry.valid
      microOpOutReg(i).bits := queueEntriesIO(entry.bits).microOpOut
    } otherwise when (io.flush || reset.asBool()) {
      microOpOutReg(i).valid := false.B
    }
    queueEntriesIO(entry.bits).select := io.microOpOut(i).fire
  }

  for (i <- 0 until decodeWidth) {
    val entry = entryEmpty._1(i)
    queueEntriesIO(entry.bits).microOpIn.bits := io.microOpIn.bits(i).bits
    queueEntriesIO(entry.bits).microOpIn.valid := io.microOpIn.fire() && io.microOpIn.bits(i).valid && entry.valid
  }
}
