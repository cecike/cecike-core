package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.IssueMicroOp
import cecike.utils._

class NaiveIssueQueue(fuNum: Int, depth: Int) extends IssueQueue(fuNum, depth) {
  require(fuNum > 0)
  require(depth > 0)

  val queueEntries = Reg(Vec(depth, Valid(new IssueMicroOp)))
  when (reset.asBool() || io.flush) {
    queueEntries.foreach(_.valid := false.B)
  }

  def queueRs1Match(i: Int) = io.readyRdMask(queueEntries(i).bits.rs1Info.addr)
  def queueRs2Match(i: Int) = io.readyRdMask(queueEntries(i).bits.rs2Info.addr)
  def queueRs1Busy(i: Int) = queueEntries(i).bits.rs1Info.busy && !queueRs1Match(i)
  def queueRs2Busy(i: Int) = queueEntries(i).bits.rs2Info.busy && !queueRs2Match(i)
  def queueReady(i: Int) = queueEntries(i).valid && !queueRs1Busy(i) && !queueRs2Busy(i)

  for(i <- 0 until depth) {
    when (queueEntries(i).valid) {
      queueEntries(i).bits.rs1Info.busy := queueRs1Busy(i)
      queueEntries(i).bits.rs2Info.busy := queueRs2Busy(i)
    }
  }

  // Pipeline buffer
  val microOpOutReg = Reg(Vec(fuNum, Valid(new IssueMicroOp)))

  // Select logic
  val entryEmptyOH = Wire(UInt(depth.W))
  entryEmptyOH := Cat(queueEntries.map(p => !p.valid).reverse)
  val entryReadyOH = Cat((0 until depth).map(queueReady).reverse)
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

  log("EntryEmpty: %x", entryEmptyOH)
  log("EntryReady: %x", entryReadyOH)

  for (i <- 0 until fuNum) {
    val entry = entryReady._1(i)

    when (io.flush || reset.asBool()) {
      microOpOutReg(i).valid := false.B
    } otherwise when ((io.microOpOut(i).fire() || !io.microOpOut(i).valid)) {
      microOpOutReg(i).valid := entry.valid
      microOpOutReg(i).bits := queueEntries(entry.bits).bits

      when (entry.valid) {
        queueEntries(entry.bits).valid := false.B
      }
      log(entry.valid, "Select mOp at %d to port %d", entry.bits, i.U)
      log(!entry.valid, "Select invalid mOp at %d to port %d", entry.bits, i.U)
    }
  }

  for (i <- 0 until decodeWidth) {
    val entry = entryEmpty._1(i)
    queueEntries(entry.bits).bits := io.microOpIn.bits(i).bits
    queueEntries(entry.bits).valid := io.microOpIn.fire() &&
      io.microOpIn.bits(i).valid && entry.valid
    log(io.microOpIn.fire() &&
      io.microOpIn.bits(i).valid && entry.valid, p"Write new mOp to ${entry.bits}")
  }

  for (i <- 0 until fuNum) {
    io.microOpOut(i).valid := microOpOutReg(i).valid && !io.flush
    io.microOpOut(i).bits := microOpOutReg(i).bits
  }
}
