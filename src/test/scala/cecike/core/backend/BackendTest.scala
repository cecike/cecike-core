package cecike.core.backend

import cecike.core.common.Constants._
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

class BranchBackendTest(c: Backend) extends PeekPokeTester(c) {
  poke(c.io.instruction.valid, true)
  val inst = 0x63 // BEQ r0, r0, 0
  poke(c.io.instruction.bits(0).pc, 114514)
  poke(c.io.instruction.bits(0).branchPredictionInfo.taken, false)
  poke(c.io.instruction.bits(0).valid, true)
  poke(c.io.instruction.bits(0).instruction, inst)
  poke(c.io.instruction.bits(1).valid, false)
  poke(c.io.instruction.bits(1).instruction, inst)
  expect(c.io.instruction.ready, true)
  step(1)
  poke(c.io.instruction.valid, false)

  var i = true
  var b = 0
  while (i && b < 20) {
    if (peek(c.io.pc.valid) == 1) {
      expect(c.io.pc.bits, 114514)
      i = false
    } else {
      step(1)
      b += 1
    }
  }
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
      Driver(() => new Backend, backendName, verbose = verboseTest) {
        c => new SimpleBackendTest(c)
      } should be(true)
    }
    "Backend" should s"works fine with branch instructions (with $backendName)" in {
      Driver(() => new Backend, backendName, verbose = verboseTest) {
        c => new BranchBackendTest(c)
      } should be(true)
    }
  }
}
