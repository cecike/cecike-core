package cecike.core.backend.issue

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._
import cecike.utils._

class ValidIssueMicroOp extends Bundle {
  val valid = Bool()
  val bits = new IssueMicroOp
}

class DispatcherIO extends Bundle {
  val functionUnitType = Input(Vec(issueClusterNum, UInt(FunctionUnitType.fuTypeWidth.W)))
  val microOpIn = Flipped(DecoupledIO(Vec(decodeWidth, Valid(new IssueMicroOp))))
  val microOpOut = Vec(issueClusterNum, DecoupledIO(Vec(decodeWidth, Valid(new IssueMicroOp))))
}

// Dispatch micro ops into issue queues, note that
// we assume the fu type of issue queues are disjoint.
class Dispatcher extends Module {
  val io = IO(new DispatcherIO)

  io.microOpOut.foreach(_.valid := io.microOpIn.valid)
  io.microOpIn.ready := io.microOpOut.map(_.ready).reduce(_&&_)

  val result = Wire(Vec(issueClusterNum, Vec(decodeWidth, new ValidIssueMicroOp)))
  result.foreach { p =>
    for (i <- 0 until decodeWidth) {
      p(i).bits := DontCare
      p(i).valid := false.B
    }
  }

  for (i <- 0 until issueClusterNum) {
    val currentIndex = Wire(Vec(decodeWidth + 1, UInt(log2Ceil(decodeWidth).W)))
    currentIndex(0) := 0.U
    for (j <- 0 until decodeWidth) {
      val valid = io.microOpIn.bits(j).valid &&
        (io.microOpIn.bits(j).bits.fuType & io.functionUnitType(i)).orR
      when (valid) {
        result(i)(currentIndex(j)).bits := io.microOpIn.bits(j).bits
        result(i)(currentIndex(j)).valid := true.B
      }
      currentIndex(j + 1) := currentIndex(j) + valid.asUInt
    }
  }

  for (i <- 0 until issueClusterNum) {
    for (j <- 0 until decodeWidth) {
      io.microOpOut(i).bits(j).valid := result(i)(j).valid
      io.microOpOut(i).bits(j).bits := result(i)(j).bits
    }
  }
}
