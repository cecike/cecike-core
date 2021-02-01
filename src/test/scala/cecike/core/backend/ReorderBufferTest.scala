package cecike.core.backend

import chisel3._
import chisel3.util._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import cecike.core.common.Constants._

class ReorderBufferSimpleTest(c: ReorderBuffer) extends PeekPokeTester(c) {
  // Put data to empty buffer
  poke(c.io.microOpIn.valid, true)
  for (i <- 0 until decodeWidth) {
    poke(c.io.microOpIn.bits(i).pc, 114514 + (i << 2))
    poke(c.io.microOpIn.bits(i).valid, true)
    poke(c.io.microOpIn.bits(i).orderInfo.rdInfo, 0)
    poke(c.io.microOpIn.bits(i).rdValid, true)
    poke(c.io.microOpIn.bits(i).instruction, i << 7)
    poke(c.io.microOpIn.bits(i).oldPhysicalRd, i)
    poke(c.io.microOpIn.bits(i).physicalRd, i + robBankNum)
  }
  expect(c.io.microOpIn.ready, true)
  step(1)
  poke(c.io.microOpIn.valid, false)
  expect(c.io.debug.bufferHead, 0)
  expect(c.io.debug.bufferTail, 1)
  expect(c.io.debug.shouldCommit, false)
  expect(c.io.debug.bufferFlushed, 0)
  expect(c.io.debug.currentEntry.basePC, 114514)
  for (i <- 0 until decodeWidth) {
    expect(c.io.debug.currentEntry.microOp(i).logicalRd, i)
    expect(c.io.debug.currentEntry.microOp(i).oldPhysicalRd, i)
    expect(c.io.debug.currentEntry.microOp(i).physicalRd, i + robBankNum)
  }
  // Make buffer full
  for (i <- 1 until robRowNum) {
    poke(c.io.microOpIn.valid, true)
    for (j <- 0 until decodeWidth) {
      poke(c.io.microOpIn.bits(j).pc, 114514 + ((i * decodeWidth + j) << 2))
      poke(c.io.microOpIn.bits(j).instruction, j << 7)
      poke(c.io.microOpIn.bits(j).oldPhysicalRd, j + i)
      poke(c.io.microOpIn.bits(j).physicalRd, j + i +robBankNum)
    }
    step(1)
  }
  poke(c.io.microOpIn.valid, false)
  expect(c.io.microOpIn.ready, false)
  // Clear first entry ... slowly
  for (i <- 0 until decodeWidth - 1) {
    poke(c.io.robReady(i).valid, true)
    poke(c.io.robReady(i).bits, i)
    step(1)
    poke(c.io.robReady(i).valid, false)
    expect(c.io.debug.shouldCommit, false)
  }
  poke(c.io.robReady(decodeWidth - 1).valid, true)
  poke(c.io.robReady(decodeWidth - 1).bits, decodeWidth - 1)

  println(peek(c.io.debug.commitReady).toString(16))
  println(peek(c.io.debug.commitDone).toString(16))

  expect(c.io.debug.shouldCommit, true)
}

class ReorderBufferTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  } else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "ReorderBuffer" should s"works fine (with $backendName)" in {
      Driver(() => new ReorderBuffer, backendName, verbose = verboseTest) {
        c => new ReorderBufferSimpleTest(c)
      } should be(true)
    }
  }
}
