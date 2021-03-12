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

  // Select logic
  val entryEmptyOH = Cat(queueEntries.map(p => !p.valid).reverse)
  val entryReadyOH = Cat(queueReady.reverse)
  val entryMaskOH = for (i <- 0 until fuNum) yield {
    Cat(queueEntries.map { p =>
      (p.bits.fuType & io.fuTypes(i)).orR()
    }.reverse)
  }
  val valid = io.microOpIn.fire()

  val entryEmpty = MultiBinaryPriorityEncoder(entryEmptyOH, decodeWidth)
  val entryReady = MultiBinaryPriorityEncoder(entryReadyOH, entryMaskOH, fuNum)
  io.microOpIn.ready := (io.microOpIn.bits zip entryEmpty._1).map { p =>
    !p._1.valid || p._2.valid
  }.reduce(_&&_)

  for (i <- 0 until fuNum) {
    val entry = entryReady._1(i)

    when (io.flush || reset.asBool()) {
      microOpOutReg(i).valid := false.B
    } otherwise when ((io.microOpOut(i).fire() || !io.microOpOut(i).valid)) {
      microOpOutReg(i).valid := entry.valid
      microOpOutReg(i).bits := queueEntries(entry.bits).bits
    }
    when (io.microOpOut(i).fire || !io.microOpOut(i).valid) {
      queueEntries(entry.bits).valid := false.B
    }
  }

  for (i <- 0 until decodeWidth) {
    val entry = entryEmpty._1(i)
    queueEntries(entry.bits).bits := io.microOpIn.bits(i).bits
    queueEntries(entry.bits).valid := io.microOpIn.fire() &&
      io.microOpIn.bits(i).valid && entry.valid
  }

  for (i <- 0 until fuNum) {
    io.microOpOut(i).valid := microOpOutReg(i).valid && !io.flush
    io.microOpOut(i).bits := microOpOutReg(i).bits
  }
}
