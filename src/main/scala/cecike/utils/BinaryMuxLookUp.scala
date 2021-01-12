package cecike.utils

import chisel3._
import chisel3.util._

object BinaryMuxLookUp {
  def apply[S <: UInt, T <: Data](sel: S, in: Seq[(BitPat, T)]): (Bool, T) = {
    val map = in.map(p => (p._1 === sel, p._2))
    (map.map(_._1).reduce(_||_), Mux1H(map))
  }
}

object BinaryMuxLookUpDefault {
  def apply[S <: UInt, T <: Data](sel: S, default: T, in: Seq[(BitPat, T)]): T = {
    val inner = BinaryMuxLookUp(sel, in)
    Mux(inner._1, inner._2, default)
  }
}
