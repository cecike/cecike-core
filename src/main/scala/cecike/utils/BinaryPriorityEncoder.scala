package cecike.utils

import chisel3._
import chisel3.util._

object CommonBinaryPriorityEncoder {
  def apply(in: Bits): (Valid[UInt], UInt) = {
    require(in.getWidth > 0)
    val roundedInputWidth = 1 << log2Ceil(in.getWidth)
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
  def apply(in: UInt, mask: Seq[UInt], n: Int): (Vec[Valid[UInt]], UInt) = {
    require(in.getWidth > 0 && in.getWidth >= n && n > 0)
    require(mask.length == n)
    require(mask.map(_.getWidth == in.getWidth).reduce(_&_))

    val outputWidth = log2Ceil(in.getWidth)
    val result = Wire(Vec(n, Valid(UInt(outputWidth.W))))
    val resultOH = Wire(Vec(n, UInt(in.getWidth.W)))
    val temp = Wire(Vec(n, UInt(in.getWidth.W)))

    val commonResult = CommonBinaryPriorityEncoder(in & mask.head)
    result(0) := commonResult._1
    resultOH(0) := commonResult._2
    temp(0) := in & (~resultOH(0)).asUInt

    for (i <- 1 until n) {
      val commonResultI = CommonBinaryPriorityEncoder(temp(i - 1) & mask(i))
      result(i) := commonResultI._1
      resultOH(i) := commonResultI._2
      temp(i) := temp(i - 1) & (~resultOH(i)).asUInt
    }

    (result, resultOH.reduce(_|_))
  }

  def apply(in: UInt, n: Int): (Vec[Valid[UInt]], UInt) = {
    val mask = Wire(Vec(n, UInt(in.getWidth.W)))
    for (i <- 0 until n) {
      mask(i) := Fill(in.getWidth, true.B)
    }
    apply(in, mask, n)
  }
}

object BinaryOHToUInt {
  def apply(in: Bits) = {
    BinaryPriorityEncoder(in).bits
  }
}
