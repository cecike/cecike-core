package cecike.utils

import chisel3._
import chisel3.util._

object AgeOrderedBinaryPriorityEncoder {
  def apply(in: UInt): Valid[UInt] = {
    require(isPow2(in.getWidth))
    val w = in.getWidth

    val result = Wire(Valid(UInt(log2Ceil(w).W)))
    result := DontCare

    when (in.andR) {
      result.valid := true.B
      result.bits := 0.U
    } otherwise {
      val rev = ReversedBinaryPriorityEncoder(~in)
      result.valid := rev.valid && !rev.bits.andR
      result.bits := rev.bits + 1.U
    }

    result
  }
}

object MultiAgeOrderedBinaryPriorityEncoder {
  def apply(in: UInt, n: Int): Vec[Valid[UInt]] = {
    require(n > 0)
    require(in.getWidth >= n)

    val first = AgeOrderedBinaryPriorityEncoder(in)

    val result = Wire(Vec(n, Valid(UInt(log2Ceil(in.getWidth).W))))
    result(0) := first

    for (i <- 1 until n) {
      result(i).bits := result(i - 1).bits + 1.U
      result(i).valid := result(i - 1).valid && result(i).bits =/= 0.U
    }

    result
  }
}
