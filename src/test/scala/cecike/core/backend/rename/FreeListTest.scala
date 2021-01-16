package cecike.core.backend.rename

import cecike.core.common.Constants.useSmallCecike
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class FreeListTest(c: FreeList) extends PeekPokeTester(c) {
  for(i <- 0 until 4) {
    poke(c.io.allocateReq(i), true)
  }
  expect(c.io.allocateResp.valid, true)
  expect(c.io.allocateResp.bits(0), 1)
  expect(c.io.allocateResp.bits(1), 64)
  expect(c.io.allocateResp.bits(2), 63)
  expect(c.io.allocateResp.bits(3), 127)
  step(1)
  expect(c.io.allocateResp.valid, true)
  expect(c.io.allocateResp.bits(0), 2)
  expect(c.io.allocateResp.bits(1), 65)
  expect(c.io.allocateResp.bits(2), 62)
  expect(c.io.allocateResp.bits(3), 126)
  for(i <- 0 until 4) {
    poke(c.io.allocateReq(i), false)
  }
  poke(c.io.deallocateReq(0).bits, 1)
  poke(c.io.deallocateReq(0).valid, true)
  step(1)
  poke(c.io.allocateReq(0), true)
  expect(c.io.allocateResp.valid, true)
  expect(c.io.allocateResp.bits(0), 1)
}

class FreeListTestForSmallCecike(c: FreeList) extends PeekPokeTester(c) {
  for(i <- 0 until 2) {
    poke(c.io.allocateReq(i), true)
  }
  expect(c.io.allocateResp.valid, true)
  expect(c.io.allocateResp.bits(0), 1)
  expect(c.io.allocateResp.bits(1), 32)
  step(1)
  expect(c.io.allocateResp.valid, true)
  expect(c.io.allocateResp.bits(0), 2)
  expect(c.io.allocateResp.bits(1), 33)
  for(i <- 0 until 2) {
    poke(c.io.allocateReq(i), false)
  }
  poke(c.io.deallocateReq(0).bits, 1)
  poke(c.io.deallocateReq(0).valid, true)
  step(1)
  poke(c.io.allocateReq(0), true)
  expect(c.io.allocateResp.valid, true)
  expect(c.io.allocateResp.bits(0), 1)
}

class FreeListTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  }
  else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "FreeList" should s"works fine (with $backendName)" in {
      Driver(() => new FreeList, backendName) {
        c => if (useSmallCecike) new FreeListTestForSmallCecike(c) else new FreeListTest(c)
      } should be(true)
    }
  }
}
