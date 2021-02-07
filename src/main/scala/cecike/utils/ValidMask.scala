package cecike.utils

import chisel3._

object ValidMask {
  def apply(length: Int, head: UInt, tail: UInt, full: Bool = false.B) = {
    def getMask(i: UInt) = {
      ((~0.U(length.W)) << i).asUInt()(length - 1, 0)
    }
    val headMask = getMask(head)
    val tailMask = (~getMask(tail)).asUInt
    Mux(head > tail, headMask | tailMask,
      Mux(full && head === tail,  (~0.U(length.W)).asUInt, headMask & tailMask))
  }
}
