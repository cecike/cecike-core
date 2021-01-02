package cecike.utils

import chisel3._
import chisel3.util._

object ReversedBinaryPriorityEncoder {
  def apply(in: Bits): Valid[UInt] = {
    require(in.getWidth > 0)
    val roundedInputWidth = 1 << log2Ceil(in.getWidth)
    print(roundedInputWidth)
    val roundedInput = Wire(UInt(roundedInputWidth.W))
    roundedInput := in
    checkedApply(roundedInput, roundedInputWidth)
  }

  def checkedApply(in: UInt, width: Int): Valid[UInt] = {
    val outputWidth = log2Ceil(width)
    val result = Wire(Valid(UInt(outputWidth.W)))

    if (width == 2) {
      val idx = in(1)
      val valid = in.orR()
      result.bits := idx
      result.valid := valid
    } else {
      val leftWidth = width >> 1
      val leftResult = checkedApply(in(leftWidth - 1, 0), leftWidth)
      val rightResult = checkedApply(in(width - 1, leftWidth), leftWidth)

      result.valid := leftResult.valid || rightResult.valid
      result.bits := Mux(rightResult.valid, Cat(true.B, rightResult.bits), leftResult.bits)
    }
    result
  }
}
