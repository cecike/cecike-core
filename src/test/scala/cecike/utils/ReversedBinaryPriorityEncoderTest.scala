package cecike.utils

import chisel3._
import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class ReversedBinaryPriorityEncoderModule extends Module {
  val io = IO(new Bundle() {
    val in = Input(UInt(64.W))
    val o = Output(Valid(UInt(6.W)))
    val lib = Output(UInt(6.W))
    val ok = Output(Bool())
  })

  io.o := ReversedBinaryPriorityEncoder(io.in)

  val chiselLibOutput = 63.U - PriorityEncoder(io.in.asBools().reverse)
  val chiselLibValid = Mux(io.in === 0.U, false.B, true.B)

  io.lib := chiselLibOutput
  io.ok := (io.o.valid === chiselLibValid) && (io.o.bits === chiselLibOutput)
}

class ReversedBinaryPriorityEncoderTest(c: ReversedBinaryPriorityEncoderModule) extends PeekPokeTester(c) {
  for(i <- 1 until 1024) {
    poke(c.io.in, i)
    expect(c.io.ok, true)
    step(1)
  }
}

class ReversedBinaryPriorityEncoderTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  }
  else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "BinaryPriorityEncoder" should s"act the same as PriorityEncoder in chisel (with $backendName)" in {
      Driver(() => new ReversedBinaryPriorityEncoderModule, backendName) {
        c => new ReversedBinaryPriorityEncoderTest(c)
      } should be(true)
    }
  }
}