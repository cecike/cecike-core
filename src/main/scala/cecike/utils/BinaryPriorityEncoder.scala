package cecike.utils

import chisel3._
import chisel3.util._

object BinaryPriorityEncoder {
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
      val idx = in(1) && (~in(0)).asBool
      val valid = in.orR()
      result.bits := idx
      result.valid := valid
    } else {
      val leftWidth = width >> 1
      val leftResult = checkedApply(in(leftWidth - 1, 0), leftWidth)
      val rightResult = checkedApply(in(width - 1, leftWidth), leftWidth)

      result.valid := leftResult.valid || rightResult.valid
      result.bits := Mux(leftResult.valid, leftResult.bits, Cat(rightResult.valid, rightResult.bits))
    }
    result
  }
}

object BinaryPriorityEncoderOH {
  def apply(in: Bits): UInt = {
    require(in.getWidth > 0)
    val roundedInputWidth = 1 << log2Ceil(in.getWidth)
    print(roundedInputWidth)
    val roundedInput = Wire(UInt(roundedInputWidth.W))
    roundedInput := in
    checkedApply(roundedInput, roundedInputWidth).bits
  }

  def checkedApply(in: UInt, width: Int): Valid[UInt] = {
    val result = Wire(Valid(UInt(width.W)))

    if (width == 2) {
      val idx = Cat(in(1) && !in(0), in(0)).asUInt
      val valid = in.orR()
      result.bits := idx
      result.valid := valid
    } else {
      val leftWidth = width >> 1
      val leftResult = checkedApply(in(leftWidth - 1, 0), leftWidth)
      val rightResult = checkedApply(in(width - 1, leftWidth), leftWidth)

      result.valid := leftResult.valid || rightResult.valid
      result.bits := Cat(Mux(leftResult.valid, 0.U, rightResult.bits), leftResult.bits)
    }
    result
  }
}

object BinaryOHToInt {
  def apply(in: Bits) = {
    BinaryPriorityEncoder(in).bits
  }
}