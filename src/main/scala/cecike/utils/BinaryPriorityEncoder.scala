package cecike.utils

import chisel3._
import chisel3.util._

object CommonBinaryPriorityEncoder {
  def apply(in: Bits): (Valid[UInt], UInt) = {
    require(in.getWidth > 0)
    val roundedInputWidth = 1 << log2Ceil(in.getWidth)
    print(roundedInputWidth)
    val roundedInput = Wire(UInt(roundedInputWidth.W))
    roundedInput := in
    val result = checkedApply(roundedInput, roundedInputWidth)
    (result._1, result._2(in.getWidth - 1, 0))
  }

  def checkedApply(in: UInt, width: Int): (Valid[UInt], UInt) = {
    val outputWidth = log2Ceil(width)
    val result = Wire(Valid(UInt(outputWidth.W)))
    val resultMask = Wire(UInt(width.W))

    if (width == 2) {
      val idx = in(1) && (~in(0)).asBool
      val mask = Cat(in(1) && !in(0), in(0)).asUInt
      val valid = in.orR()
      result.bits := idx
      result.valid := valid
      resultMask := mask
    } else {
      val leftWidth = width >> 1
      val leftResult = checkedApply(in(leftWidth - 1, 0), leftWidth)
      val rightResult = checkedApply(in(width - 1, leftWidth), leftWidth)

      result.valid := leftResult._1.valid || rightResult._1.valid
      result.bits := Mux(leftResult._1.valid, leftResult._1.bits, Cat(rightResult._1.valid, rightResult._1.bits))
      resultMask := Cat(Mux(leftResult._1.valid, 0.U, rightResult._2), leftResult._2)
    }
    (result, resultMask)
  }
}

object BinaryPriorityEncoder {
  def apply(in: Bits): Valid[UInt] = CommonBinaryPriorityEncoder(in)._1
}

object BinaryPriorityEncoderOH {
  def apply(in: Bits): UInt = CommonBinaryPriorityEncoder(in)._2
}

object MultiBinaryPriorityEncoder {
  def apply(in: UInt, length: Int): (Vec[Valid[UInt]], UInt) = {
    require(in.getWidth > 0 && in.getWidth >= length && length > 0)
    val outputWidth = log2Ceil(in.getWidth)
    val result = Wire(Vec(length, Valid(UInt(outputWidth.W))))
    val mask = Wire(Vec(length, UInt(in.getWidth.W)))
    val temp = Wire(Vec(length, UInt(in.getWidth.W)))

    val commonResult = CommonBinaryPriorityEncoder(in)
    result(0) := commonResult._1
    mask(0) := commonResult._2
    temp(0) := in & (~mask(0)).asUInt

    for (i <- 1 until length) {
      val commonResultI = CommonBinaryPriorityEncoder(temp(i - 1))
      result(i) := commonResultI._1
      mask(i) := commonResultI._2
      temp(i) := temp(i - 1) & (~mask(i)).asUInt
    }

    (result, mask.reduce(_|_))
  }
}

object BinaryOHToInt {
  def apply(in: Bits) = {
    BinaryPriorityEncoder(in).bits
  }
}