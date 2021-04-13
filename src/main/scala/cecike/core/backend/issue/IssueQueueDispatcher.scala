package cecike.core.backend.issue

import cecike.CecikeModule
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.{IssueMicroOp, MicroOp}
import cecike.utils._

class IssueQueueDispatcherIO extends Bundle {
  val microOpIn = DeqIO(Vec(decodeWidth, new MicroOp))
  val acceptFuTypes = Input(Vec(issueClusterNum, UInt(FunctionUnitType.fuTypeWidth.W)))
  val microOpOut = Vec(issueClusterNum, EnqIO(Vec(decodeWidth, Valid(new IssueMicroOp))))
  val currentROBAddressBase = Input(UInt(robAddressWidth.W))
  val robMicroOpOut = EnqIO(Vec(decodeWidth, new MicroOp))
}

class IssueQueueDispatcher extends CecikeModule {
  val io = IO(new IssueQueueDispatcherIO)

  assert(!io.acceptFuTypes.reduce(_&_).orR(), "Accepted fu types should be disjoint")

  for (i <- 0 until issueClusterNum) {
    def iOp(j: Int): Valid[IssueMicroOp] = {
      val t = Wire(Valid(new IssueMicroOp))
      val issueMicroOp = IssueMicroOp(io.microOpIn.bits(j))
      t.bits := issueMicroOp
      t.bits.robIndex := io.currentROBAddressBase + j.U
      t.valid := (issueMicroOp.fuType & io.acceptFuTypes(i)).orR() &&
        io.microOpIn.bits(j).valid
      t
    }

    for (j <- 0 until decodeWidth) {
      io.microOpOut(i).bits(j) := iOp(j)
    }
  }
  io.robMicroOpOut.bits := io.microOpIn.bits

  val ready = io.microOpOut.map(_.ready).reduce(_&&_) && io.robMicroOpOut.ready
  io.microOpIn.ready := ready
  io.microOpOut.foreach(_.valid := ready && io.microOpIn.valid)
  io.robMicroOpOut.valid := io.microOpIn.valid && ready
}
