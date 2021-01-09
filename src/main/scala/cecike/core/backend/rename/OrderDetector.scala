package cecike.core.backend.rename

import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common.OrderInfo
import cecike.utils._

class OrderDetectorIO extends Bundle {
  val rs1 = Input(Vec(decodeWidth, UInt(logicalRegisterAddressWidth.W)))
  val rs2 = Input(Vec(decodeWidth, UInt(logicalRegisterAddressWidth.W)))
  val rd = Input(Vec(decodeWidth, UInt(logicalRegisterAddressWidth.W)))
  val valid = Input(Vec(decodeWidth, Bool()))
  val info = Output(Vec(decodeWidth, new OrderInfo))
}

class OrderDetector extends Module {
  val io = IO(new OrderDetectorIO)

  for (i <- 0 until decodeWidth) {
    val rs1Info = Wire(Valid(UInt(log2Ceil(decodeWidth).W)))
    val rs2Info = Wire(Valid(UInt(log2Ceil(decodeWidth).W)))

    rs1Info.bits := 0.U
    rs1Info.valid := false.B

    rs2Info.bits := 0.U
    rs2Info.valid := false.B

    for (j <- 0 until i) {
      when (io.rs1(i) === io.rd(j) && io.rs1(i) =/= 0.U && io.valid(j)) {
        rs1Info.valid := true.B
        rs1Info.bits := j.U
      }
      when (io.rs2(i) === io.rd(j) && io.rs2(i) =/= 0.U && io.valid(j)) {
        rs2Info.valid := true.B
        rs2Info.bits := j.U
      }
    }

    val rdInfo = Wire(Vec(decodeWidth, UInt(decodeWidth.W)))
    rdInfo.foreach(_ := 0.U)

    for (j <- i + 1 until decodeWidth) {
      when (io.rd(i) === io.rd(j) && io.rd(i) =/= 0.U && io.valid(j)) {
        rdInfo(j) := rdInfo(j - 1) | (UIntToOH(j.U, decodeWidth))
      } otherwise {
        rdInfo(j) := rdInfo(j - 1)
      }
    }

    io.info(i).rs1Info := rs1Info
    io.info(i).rs2Info := rs2Info
    io.info(i).rdInfo := rdInfo(decodeWidth - 1)
  }
}
