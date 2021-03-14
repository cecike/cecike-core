package cecike.core.backend.lsu

import cecike.core.common.Constants._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}



class VirtualAddressTranslateFSMTest(c: VirtualAddressTranslateFSM) extends PeekPokeTester(c) {
  expect(c.io.debug.state, c.s_idle)
  expect(c.io.agu.ready, true)

  expect(c.io.tlb.virtualAddress.valid, false)
  expect(c.io.res.valid, false)

  poke(c.io.agu.valid, true)
  poke(c.io.agu.bits.address.address, 114514)

  expect(c.io.debug.nextState, c.s_shake)

  step(1)

  poke(c.io.agu.valid, false)

  expect(c.io.debug.state, c.s_shake)
  expect(c.io.tlb.virtualAddress.valid, true)
  expect(c.io.tlb.virtualAddress.bits, 114514)

  poke(c.io.tlb.virtualAddress.ready, true)
  poke(c.io.tlb.queryResult.valid, true)
  poke(c.io.tlb.queryResult.bits.physicalAddress, 114514)

  poke(c.io.res.ready, false)

  expect(c.io.debug.nextState, c.s_send)
  expect(c.io.res.valid, true)
  expect(c.io.res.bits.aguInfo.address.address, 114514)
}


class VirtualAddressTranslateFSMTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  }
  else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "OrderDetector" should s"returns empty data under dummy input (with $backendName)" in {
      Driver(() => new VirtualAddressTranslateFSM, backendName, verbose = verboseTest) {
        c => new VirtualAddressTranslateFSMTest(c)
      } should be(true)
    }
  }
}