package cecike.core.backend.rename

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class FreeListTest(c: FreeList) extends PeekPokeTester(c) {
  for(i <- 0 until 2) {
    poke(c.io.allocateReq(i), true)
  }
  expect(c.io.allocateResp.valid, true)
  expect(c.io.allocateResp.bits(0), 1)
  expect(c.io.allocateResp.bits(1), 127)
  step(1)
  expect(c.io.allocateResp.valid, true)
  expect(c.io.allocateResp.bits(0), 2)
  expect(c.io.allocateResp.bits(1), 126)
  for(i <- 0 until 2) {
    poke(c.io.allocateReq(i), false)
  }
  poke(c.io.deallocateReq(0), 2)
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
        c => new FreeListTest(c)
      } should be(true)
    }
  }
}
