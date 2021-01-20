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
  step(withSmallOption(1, 3))
  expect(c.io.allocateResp.valid, false)
  for (i <- 0 until decodeWidth) {
    poke(c.io.allocateReq(i), false)
  }
  step(1)
  // check valid
  for (i <- 0 until decodeWidth) {
    poke(c.io.readReq.branchTag, IntToOH(i))
    expect(c.io.readReq.valid, true)
    step(1)
  }
  poke(c.io.readReq.branchTag, IntToOH(maxBranchCount - 1))
  expect(c.io.readReq.valid, false)
  step(1)
  // make it invalid
  poke(c.io.branchInfo.valid, true)
  poke(c.io.branchInfo.mispredicted, true)
  poke(c.io.branchInfo.tag, IntToOH(1))
  step(1)
  poke(c.io.branchInfo.valid, false)
  for (i <- 1 until maxBranchCount - decodeWidth) {
    poke(c.io.readReq.branchTag, IntToOH(i))
    expect(c.io.readReq.valid, false)
    step(1)
  }
  // give it back
  for (i <- 0 until decodeWidth) {
    poke(c.io.deallocateReq(i), true)
  }
  step(1)
  // check again
  for (i <- 0 until decodeWidth) {
    poke(c.io.allocateReq(i), true)
  }
  expect(c.io.allocateResp.valid, true)
  for (j <- 0 until decodeWidth) {
    expect(c.io.allocateResp.bits(j), IntToOH((maxBranchCount - decodeWidth + j) % maxBranchCount))
  }
  step(1)
  for (j <- 0 until decodeWidth) {
    poke(c.io.readReq.branchTag, IntToOH((maxBranchCount - decodeWidth + j) % maxBranchCount))
    expect(c.io.readReq.valid, true)
    step(1)
  }
}

class BranchSnapshotBufferTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  }
  else {
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