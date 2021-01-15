package cecike.core.common

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.utils.UndirectionalValid

class OrderInfo extends Bundle {
  val rs1Info = UndirectionalValid(UInt(log2Ceil(decodeWidth).W))
  val rs2Info = UndirectionalValid(UInt(log2Ceil(decodeWidth).W))
  val rdInfo = UInt(decodeWidth.W)

  def validWithMask(mask: UInt, selfIndex: Int, end: Int = decodeWidth - 1): Bool = {
    val temp = (mask(end, 0)).asUInt & (~((1.U(decodeWidth.W) << selfIndex.U))).asUInt
    !(rdInfo & temp(decodeWidth - 1, 0)).orR()
  }
}
