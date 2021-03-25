package cecike.core.frontend

import cecike.core.common.Constants._
import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class MetaFrontendTest(c: Frontend) extends PeekPokeTester(c) {
  val memPort = c.io.memoryRead.data

  def pokeMemData(valid: Boolean, valid1: Boolean, bits1: BigInt, valid2: Boolean, bits2: BigInt) = {
    poke(memPort.valid, valid)
    poke(memPort.bits(0).valid, valid1)
    poke(memPort.bits(0).bits, bits1)
    poke(memPort.bits(1).valid, valid2)
    poke(memPort.bits(1).bits, bits2)
  }
  def clearMemData() = {
    poke(memPort.valid, false)
  }
}

class SimpleFrontendTest(c: Frontend) extends MetaFrontendTest(c) {
  poke(c.io.backendPC.valid, false)
  poke(c.io.instruction.ready, true)
  expect(c.io.memoryRead.addressInfo.valid, true)
  expect(c.io.memoryRead.addressInfo.bits.address, pcInitValue)
  step(1)
  pokeMemData(true, true, 114514, true, 1919810)
  expect(c.io.instruction.valid, true)
  expect(c.io.instruction.bits(0).valid, true)
  expect(c.io.instruction.bits(0).instruction, 114514)
  expect(c.io.instruction.bits(0).pc, pcInitValue)
  expect(c.io.instruction.bits(1).valid, true)
  expect(c.io.instruction.bits(1).instruction, 1919810)
  expect(c.io.instruction.bits(1).pc, pcInitLitValue + 4)
  step(2)
}

class BackendPCFrontendTest(c: Frontend) extends MetaFrontendTest(c) {
  poke(c.io.backendPC.valid, true)
  poke(c.io.backendPC.bits, 114514)
  poke(c.io.instruction.ready, true)
  expect(c.io.memoryRead.addressInfo.valid, true)
  expect(c.io.memoryRead.addressInfo.bits.address, 114514)
  step(1)
  pokeMemData(true, true, 114514, true, 1919810)
  expect(c.io.instruction.valid, true)
  expect(c.io.instruction.bits(0).valid, true)
  expect(c.io.instruction.bits(0).instruction, 114514)
  expect(c.io.instruction.bits(0).pc, 114514)
  expect(c.io.instruction.bits(1).valid, true)
  expect(c.io.instruction.bits(1).instruction, 1919810)
  expect(c.io.instruction.bits(1).pc, 114514 + 4)
  step(2)
}

class CrossCacheLineFrontendTest(c: Frontend) extends MetaFrontendTest(c) {
  poke(c.io.backendPC.valid, true)
  poke(c.io.backendPC.bits, 60)
  poke(c.io.instruction.ready, true)
  expect(c.io.memoryRead.addressInfo.valid, true)
  expect(c.io.memoryRead.addressInfo.bits.address, 60)
  step(1)
  poke(c.io.backendPC.valid, false)
  pokeMemData(true, true, 114514, false, 1919810)
  expect(c.io.instruction.valid, true)
  expect(c.io.instruction.bits(0).valid, true)
  expect(c.io.instruction.bits(0).instruction, 114514)
  expect(c.io.instruction.bits(0).pc, 60)
  expect(c.io.instruction.bits(1).valid, false)
  expect(c.io.instruction.bits(1).instruction, 1919810)
  expect(c.io.instruction.bits(1).pc, 64)
  expect(c.io.memoryRead.addressInfo.valid, true)
  expect(c.io.memoryRead.addressInfo.bits.address, 64)
  step(2)
}

class BackendStallFrontendTest(c: Frontend) extends MetaFrontendTest(c) {
  poke(c.io.backendPC.valid, false)
  poke(c.io.instruction.ready, true)
  expect(c.io.memoryRead.addressInfo.valid, true)
  expect(c.io.memoryRead.addressInfo.bits.address, pcInitValue)
  step(1)
  pokeMemData(true, true, 114514, true, 1919810)
  poke(c.io.instruction.ready, false)
  expect(c.io.instruction.valid, true)
  expect(c.io.instruction.bits(0).valid, true)
  expect(c.io.instruction.bits(0).instruction, 114514)
  expect(c.io.instruction.bits(0).pc, pcInitValue)
  expect(c.io.instruction.bits(1).valid, true)
  expect(c.io.instruction.bits(1).instruction, 1919810)
  expect(c.io.instruction.bits(1).pc, pcInitLitValue + 4)
  expect(c.io.memoryRead.addressInfo.valid, false.B)
  step(3)
  expect(c.io.instruction.valid, true)
  expect(c.io.instruction.bits(0).valid, true)
  expect(c.io.instruction.bits(0).instruction, 114514)
  expect(c.io.instruction.bits(0).pc, pcInitValue)
  expect(c.io.instruction.bits(1).valid, true)
  expect(c.io.instruction.bits(1).instruction, 1919810)
  expect(c.io.instruction.bits(1).pc, pcInitLitValue + 4)

  poke(c.io.instruction.ready, true)
  expect(c.io.memoryRead.addressInfo.valid, true.B)
  expect(c.io.memoryRead.addressInfo.bits.address, pcInitLitValue + 8)
  step(1)
  expect(c.io.instruction.valid, true)
  expect(c.io.instruction.bits(0).valid, true)
  expect(c.io.instruction.bits(0).instruction, 114514)
  expect(c.io.instruction.bits(0).pc, pcInitLitValue + 8)
  expect(c.io.instruction.bits(1).valid, true)
  expect(c.io.instruction.bits(1).instruction, 1919810)
  expect(c.io.instruction.bits(1).pc, pcInitLitValue + 12)
}

class FrontendTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  }
  else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "Frontend" should s"works fine with example input instructions (with $backendName)" in {
      Driver(() => new Frontend, backendName, verbose = verboseTest) {
        c => new SimpleFrontendTest(c)
      } should be(true)
    }
    "Frontend" should s"works fine with backend pc change (with $backendName)" in {
      Driver(() => new Frontend, backendName, verbose = verboseTest) {
        c => new BackendPCFrontendTest(c)
      } should be(true)
    }
    "Frontend" should s"works fine when address cross cache line (with $backendName)" in {
      Driver(() => new Frontend, backendName, verbose = verboseTest) {
        c => new CrossCacheLineFrontendTest(c)
      } should be(true)
    }
    "Frontend" should s"works fine when backend stalls(with $backendName)" in {
      Driver(() => new Frontend, backendName, verbose = verboseTest) {
        c => new BackendStallFrontendTest(c)
      } should be(true)
    }
  }
}
