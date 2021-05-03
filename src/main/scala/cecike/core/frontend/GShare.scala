package cecike.core.frontend

import cecike.CecikeModule
import chisel3._
import chisel3.util._
import cecike.core.common.Constants._
import cecike.core.common._

class GShareIO extends Bundle {
  val branchInfo = Input(new BranchInfo)
  val pc = Input(Vec(decodeWidth, UInt(xLen.W)))
  val branchResult = Output(Vec(decodeWidth, Bool()))
}

class GShare extends Module {
  val io = IO(new GShareIO)

  val globalHistory = RegInit(0.U(globalHistoryWidth.W))
  val branchResult = Mem(1 << globalHistoryWidth, UInt(2.W))

  def bcNextState(data: UInt, taken: Bool) = {
    MuxLookup(data, 0.U, Array(
      0.U -> Mux(taken, 1.U, 0.U),
      1.U -> Mux(taken, 3.U, 0.U),
      2.U -> Mux(taken, 3.U, 1.U),
      3.U -> Mux(taken, 3.U, 2.U)
    ))
  }

  def bcIdx(data: UInt) = {
    data(globalHistoryWidth - 1, 0)
  }

  when (io.branchInfo.valid) {
    globalHistory := (globalHistory << 1) ## io.branchInfo.taken
    val idx = bcIdx(io.branchInfo.pc)
    branchResult.write(idx, bcNextState(branchResult(idx), io.branchInfo.taken))
  }

  (io.branchResult zip io.pc).foreach { p =>
    p._1 := RegNext(branchResult(p._2)(1))
  }
}
