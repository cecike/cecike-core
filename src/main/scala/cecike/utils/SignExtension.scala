package cecike.utils

import chisel3._

object SignExtension {
  def apply(data: Bits, width: Int) = {
    val result = Wire(SInt(width.W))
    result := data.asSInt
    result.asUInt
  }

  def apply(data: Bits): UInt = apply(data, 64)
}
