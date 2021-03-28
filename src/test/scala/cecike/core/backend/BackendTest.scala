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

class LoadBackendTest(c: Backend) extends PeekPokeTester(c) {
  poke(c.io.instruction.valid, true)
  val inst = 0x20206083 // LWU r1, 514(r0)
  poke(c.io.instruction.bits(0).pc, 114514)
  poke(c.io.instruction.bits(0).branchPredictionInfo.taken, false)
  poke(c.io.instruction.bits(0).valid, true)
  poke(c.io.instruction.bits(0).instruction, inst)
  poke(c.io.instruction.bits(1).valid, false)
  poke(c.io.instruction.bits(1).instruction, inst)
  expect(c.io.instruction.ready, true)
  step(1)
  poke(c.io.instruction.valid, false)
  poke(c.io.memoryRead.addressInfo.ready, false)
  poke(c.io.memoryRead.data.bits, 1919810)

  var i = true
  var b = 0
  poke(c.io.tlbQuery.virtualAddress.ready, true)
  poke(c.io.memoryRead.addressInfo.ready, true)
  poke(c.io.memoryRead.data.valid, true)
  while (i && b < 10) {
    if (peek(c.io.tlbQuery.virtualAddress.valid) == 1) {
      expect(c.io.tlbQuery.virtualAddress.bits, 514)
      poke(c.io.tlbQuery.queryResult.valid, true)
      poke(c.io.tlbQuery.queryResult.bits.valid, true)
      poke(c.io.tlbQuery.queryResult.bits.physicalAddress, 114514)
      step(1)
      b += 1
    } else if (peek(c.io.memoryRead.addressInfo.valid) == 1) {
      expect(c.io.memoryRead.addressInfo.bits.address, 114514)
      expect(c.io.memoryRead.addressInfo.bits.size, 2)
      step(1)
      b += 1
    } else {
      step(1)
      b += 1
    }
  }

  poke(c.io.debug.register.addr, 1)
  expect(c.io.debug.register.data, 1919810)
}

class StoreBackendTest(c: Backend) extends PeekPokeTester(c) {
  poke(c.io.instruction.valid, true)
  val inst1 = 0x00100093 // ADDI r1, r0, 1
  val inst2 = 0x20102123 // SW r1, 514(r0)
  poke(c.io.instruction.bits(0).pc, 114514)
  poke(c.io.instruction.bits(0).branchPredictionInfo.taken, false)
  poke(c.io.instruction.bits(0).valid, true)
  poke(c.io.instruction.bits(0).instruction, inst1)
  poke(c.io.instruction.bits(1).valid, true)
  poke(c.io.instruction.bits(1).instruction, inst2)
  expect(c.io.instruction.ready, true)
  step(1)
  poke(c.io.instruction.valid, false)
  poke(c.io.memoryWrite.storeInfo.ready, true)

  var i = true
  var b = 0
  poke(c.io.tlbQuery.virtualAddress.ready, true)
  poke(c.io.memoryRead.addressInfo.ready, true)
  poke(c.io.memoryRead.data.valid, true)
  while (i && b < 10) {
    if (peek(c.io.tlbQuery.virtualAddress.valid) == 1) {
      expect(c.io.tlbQuery.virtualAddress.bits, 514)
      poke(c.io.tlbQuery.queryResult.valid, true)
      poke(c.io.tlbQuery.queryResult.bits.valid, true)
      poke(c.io.tlbQuery.queryResult.bits.physicalAddress, 114514)
      step(1)
      b += 1
    } else if (peek(c.io.memoryWrite.storeInfo.valid) == 1) {
      expect(c.io.memoryWrite.storeInfo.bits.addressInfo.address, 114514)
      expect(c.io.memoryWrite.storeInfo.bits.addressInfo.size, 2)
      expect(c.io.memoryWrite.storeInfo.bits.data, 1)
      step(1)
      b += 1
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
    "Backend" should s"works fine with load instructions (with $backendName)" in {
      Driver(() => new Backend, backendName, verbose = verboseTest) {
        c => new LoadBackendTest(c)
      } should be(true)
    }
    "Backend" should s"works fine with store instructions (with $backendName)" in {
      Driver(() => new Backend, backendName, verbose = verboseTest) {
        c => new StoreBackendTest(c)
      } should be(true)
    }
  }
}
