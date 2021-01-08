package cecike.core.backend.rename

import cecike.core.common.Constants._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class EmptyOrderDetectorTest(c: OrderDetector) extends PeekPokeTester(c) {
  val rs1Data = Array(0, 0, 0, 0)
  val rs2Data = Array(0, 0, 0, 0)
  val rdData = Array(0, 0, 0, 0)
  val validData = Array(true, true, true, true)

  val infoRs1Valid = Array(false, false, false, false)
  val infoRs1Data = Array(0, 0, 0, 0)
  val infoRs2Valid = Array(false, false, false, false)
  val infoRs2Data = Array(0, 0, 0, 0)
  val infoRdData = Array(0, 0, 0, 0)

  for (i <- 0 until decodeWidth) {
    poke(c.io.rs1(i), rs1Data(i))
    poke(c.io.rs2(i), rs2Data(i))
    poke(c.io.rd(i), rdData(i))
    poke(c.io.valid(i), validData(i))
  }
  step(1)
  for (i <- 0 until decodeWidth) {
    expect(c.io.info(i).rs1Info.valid, infoRs1Valid(i))
    expect(c.io.info(i).rs1Info.bits, infoRs1Data(i))
    expect(c.io.info(i).rs2Info.valid, infoRs2Valid(i))
    expect(c.io.info(i).rs2Info.bits, infoRs2Data(i))
    expect(c.io.info(i).rdInfo, infoRdData(i))
  }
}

class SimpleOrderDetectorTest(c: OrderDetector) extends PeekPokeTester(c) {
  val rs1Data = Array(0, 1, 1, 0)
  val rs2Data = Array(0, 0, 0, 2)
  val rdData = Array(1, 1, 2, 0)
  val validData = Array(true, true, true, true)

  val infoRs1Valid = Array(false, true, true, false)
  val infoRs1Data = Array(0, 0, 1, 0)
  val infoRs2Valid = Array(false, false, false, true)
  val infoRs2Data = Array(0, 0, 0, 2)
  val infoRdData = Array(2, 0, 0, 0)

  for (i <- 0 until decodeWidth) {
    poke(c.io.rs1(i), rs1Data(i))
    poke(c.io.rs2(i), rs2Data(i))
    poke(c.io.rd(i), rdData(i))
    poke(c.io.valid(i), validData(i))
  }
  step(1)
  for (i <- 0 until decodeWidth) {
    expect(c.io.info(i).rs1Info.valid, infoRs1Valid(i))
    expect(c.io.info(i).rs1Info.bits, infoRs1Data(i))
    expect(c.io.info(i).rs2Info.valid, infoRs2Valid(i))
    expect(c.io.info(i).rs2Info.bits, infoRs2Data(i))
    expect(c.io.info(i).rdInfo, infoRdData(i))
  }
}

class OrderDetectorTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("verilator")
  }
  else {
    Array("firrtl")
  }

  for (backendName <- backendNames) {
    "OrderDetector" should s"returns empty data under dummy input (with $backendName)" in {
      Driver(() => new OrderDetector, backendName) {
        c => new EmptyOrderDetectorTest(c)
      } should be(true)
    }
    "OrderDetector" should s"returns works fine under simple input (with $backendName)" in {
      Driver(() => new OrderDetector, backendName) {
        c => new SimpleOrderDetectorTest(c)
      } should be(true)
    }
  }
}