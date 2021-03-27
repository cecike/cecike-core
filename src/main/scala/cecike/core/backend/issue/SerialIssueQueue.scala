package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.IssueMicroOp
import cecike.utils._

class SerialIssueQueueEntry extends Bundle {
  val valid = Bool()
  val bits = new IssueMicroOp
}

class SerialIssueQueue(depth: Int) extends IssueQueue(1, depth) {
  require(isPow2(depth))

  val queueEntries = Mem(depth, new SerialIssueQueueEntry)
  val queueManager = Module(new RingBufferManager(depth, decodeWidth, 1))
  queueManager.io.clear := io.flush
  queueManager.io.req.valid := io.microOpIn.valid

  // Input
  for (i <- 0 until decodeWidth) {
    queueManager.io.req.bits(i) := io.microOpIn.bits(i).valid
  }
  io.microOpIn.ready := queueManager.io.resp.valid

  for (i <- 0 until decodeWidth) {
    val entry = queueEntries(queueManager.io.resp.bits(i))
    entry.bits := io.microOpIn.bits(i).bits
    entry.valid := io.microOpIn.fire && io.microOpIn.bits(i).valid
    log(io.microOpIn.fire && io.microOpIn.bits(i).valid,
      "Write new mOp of index %d to %d",
      io.microOpIn.bits(i).bits.robIndex, queueManager.io.resp.bits(i))
  }

  // Output
  val outputEntry = queueEntries(queueManager.io.head)
  io.microOpOut(0).bits := outputEntry.bits
  io.microOpOut(0).valid := !io.flush &&
    outputEntry.valid &&
    !io.busyTable(outputEntry.bits.rs1Info.addr) &&
    !io.busyTable(outputEntry.bits.rs2Info.addr)
  queueManager.io.deallocate(0) := io.microOpOut(0).fire
  log(p"${io.microOpOut(0).valid}")
  log(io.microOpOut(0).fire, "Select entry %d to fire", queueManager.io.head)

  log("Head: %d Tail: %d", queueManager.io.head, queueManager.io.tail)
}
