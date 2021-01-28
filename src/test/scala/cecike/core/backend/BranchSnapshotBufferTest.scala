package cecike.core.backend

import chisel3._
import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import cecike.core.common.Constants._

class BranchSnapshotBufferSimpleTest(c: BranchSnapshotBuffer) extends PeekPokeTester(c) {
  def IntToOH(i: Int) = {
    1 << i
  }
  // Simple allocate
  for (i <- 0 until decodeWidth) {
    poke(c.io.allocateReq(i), true)
  }
  expect(c.io.allocateResp.valid, true)
  for (j <- 0 until decodeWidth) {
    expect(c.io.allocateResp.bits(j), IntToOH(j))
  }
  step(3)
  expect(c.io.allocateResp.valid, false)
  for (i <- 0 until decodeWidth) {
    poke(c.io.allocateReq(i), false)
  }
}

class BranchSnapshotBufferTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  } else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "BranchSnapshotBuffer" should s"works fine (with $backendName)" in {
      Driver(() => new BranchSnapshotBuffer, backendName, verbose = verboseTest) {
        c => new BranchSnapshotBufferSimpleTest(c)
      } should be(true)
    }
  }
}
