package cecike.utils

import chisel3._
import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

object ChiselLibMultiSelect {
  def apply(in: UInt, n: Int) = {
    val sels = Wire(Vec(n, UInt(in.getWidth.W)))
    var temp = in
    var mask = 0.U(in.getWidth.W)

    for (i <- 0 until n) {
      val t = PriorityEncoderOH(temp)
      mask = mask | t
      sels(i) := OHToUInt(t)
      temp = temp & (~t).asUInt
    }

    (sels, mask)
  }
}

object PopCount {
  def apply(n: Int): Int = {
    if (n < 0) {
      0
    } else {
      var result = 0
      var temp = n
      while(temp != 0) {
        if (temp % 2 == 1) {
          result += 1
        }
        temp >>= 1
      }
      result
    }
  }
}

class MultiBinaryPriorityEncoderModule extends Module {
  val io = IO(new Bundle() {
    val in = Input(UInt(64.W))
    val ok = Output(Bool())
    val util = Output(Vec(4, UInt(6.W)))
    val lib = Output(Vec(4, UInt(6.W)))
  })
  val util = MultiBinaryPriorityEncoder(io.in, 4)
  for(i <- 0 until 4) {
    io.util(i) := util._1(i).bits
  }

  val lib = ChiselLibMultiSelect(io.in, 4)
  io.lib := lib._1
  io.ok := (util._2 === lib._2) && (util._1 zip lib._1).map(p => p._1.bits === p._2).reduce(_&_)
}


class MultiBinaryPriorityEncoderTest(c: MultiBinaryPriorityEncoderModule) extends PeekPokeTester(c) {
  for(i <- 1 until 4096) {
    if (PopCount(i) >= 4) {
      poke(c.io.in, i)

      expect(c.io.ok, true)
      step(1)
    }
  }
}

class MultiBinaryPriorityEncoderTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  }
  else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "BinaryPriorityEncoder" should s"act the same as PriorityEncoder in chisel (with $backendName)" in {
      Driver(() => new MultiBinaryPriorityEncoderModule, backendName) {
        c => new MultiBinaryPriorityEncoderTest(c)
      } should be(true)
    }
  }
}
