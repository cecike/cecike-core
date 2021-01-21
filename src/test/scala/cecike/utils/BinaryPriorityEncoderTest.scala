package cecike.utils

import chisel3._
import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class BinaryPriorityEncoderModule extends Module {
  val io = IO(new Bundle() {
    val in = Input(UInt(64.W))
    val o = Output(Valid(UInt(6.W)))
    val lib = Output(UInt(6.W))
    val ok = Output(Bool())
  })

  io.o := BinaryPriorityEncoder(io.in)

  val utilOH = BinaryPriorityEncoderOH(io.in)

  val chiselLibOutput = PriorityEncoder(io.in)
  val chiselLibValid = Mux(io.in === 0.U, false.B, true.B)

  io.lib := chiselLibOutput
  io.ok := (io.o.valid === chiselLibValid) && (io.o.bits === chiselLibOutput) && (io.o.bits === OHToUInt(utilOH))
}

class BinaryPriorityEncoderTest(c: BinaryPriorityEncoderModule) extends PeekPokeTester(c) {
  for(i <- 1 until 1024) {
    poke(c.io.in, i)
    expect(c.io.ok, true)
    step(1)
  }
}

class BinaryPriorityEncoderTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  } else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "BinaryPriorityEncoder" should s"act the same as PriorityEncoder in chisel (with $backendName)" in {
      Driver(() => new BinaryPriorityEncoderModule, backendName) {
        c => new BinaryPriorityEncoderTest(c)
      } should be(true)
    }
  }
}
