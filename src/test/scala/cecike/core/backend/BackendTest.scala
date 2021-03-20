package cecike.core.backend

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class SimpleBackendTest(c: Backend) extends PeekPokeTester(c) {
  poke(c.io.instruction.valid, true)
  val inst = 0x00100093 // ADDI r1, r0, 1
  poke(c.io.instruction.bits(0).valid, true)
  poke(c.io.instruction.bits(0).instruction, inst)
  poke(c.io.instruction.bits(1).valid, true)
  poke(c.io.instruction.bits(1).instruction, inst)
  expect(c.io.instruction.ready, true)
  step(1)
  poke(c.io.instruction.valid, false)
  step(20)
  poke(c.io.debug.register.addr, 1)
  expect(c.io.debug.register.data, 1)
  poke(c.io.debug.register.addr, 127)
  expect(c.io.debug.register.data, 1)
}

class BackendTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  }
  else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "Backend" should s"works fine with example input instructions (with $backendName)" in {
      Driver(() => new Backend, backendName, verbose = true) {
        c => new SimpleBackendTest(c)
      } should be(true)
    }
  }
}
