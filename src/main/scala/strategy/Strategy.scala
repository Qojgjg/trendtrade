package strategy

import java.time.LocalDateTime
import scala.collection.mutable

case class Kline(
    datetime: LocalDateTime,
    open: BigDecimal,
    high: BigDecimal,
    low: BigDecimal,
    close: BigDecimal,
    hold: BigDecimal
)

abstract class AStrategy {
  val klines: mutable.ListBuffer[Kline] = mutable.ListBuffer.empty
  def step(k: Kline): Unit
}

class Strategy extends AStrategy {
  def step(k: Kline): Unit = {
    klines.prepend(k)
  }
}

trait MaMixin(intervals: Vector[Int]) extends AStrategy {
  val mas: mutable.Map[Int, mutable.ListBuffer[BigDecimal]] = mutable.Map.from(
    intervals
      .map(i => {
        (i, mutable.ListBuffer.empty[BigDecimal])
      })
  )
  abstract override def step(k: Kline): Unit = {
    super.step(k)
    intervals.foreach(interval => {
      val ks = klines.slice(0, interval)
      val avg = ks.map(_.close).sum / ks.length
      mas(interval).prepend(avg)
    })
  }
}


case class Macd(
  ema12: BigDecimal,
  ema26: BigDecimal,
  diff: BigDecimal,
  dea: BigDecimal,
  bar: BigDecimal
){
  def next(price: BigDecimal, emaa: Int,emab: Int): Macd = {
    val e12 = ema12 * (emaa - 1) / (emaa + 1) + price * 2 / (emaa + 1)
    val e26 = ema26 * (emab - 1) / (emab + 1) + price * 2 / (emab +1)
    val newDif = e12 - e26
    val newDea = this.dea * 8 / 10 + newDif * 2 / 10
    val b = 2 *(newDif - newDea)
    Macd(e12,e26,newDif,newDea,b)
  }
}

trait MacdMixin(fast: Int = 12, slow: Int = 26) extends AStrategy {
  val macd: mutable.ListBuffer[Macd] = mutable.ListBuffer.empty

  abstract override def step(k: Kline): Unit = {
    super.step(k)
    if(klines.length == 1) {
      macd.prepend(Macd(k.close, k.close, 0, 0, 0))
    }else{
      macd.prepend(macd(0).next(k.close, fast,slow))
    }
  }
}

case class Kdj(
  kline: Kline,
  rsv: BigDecimal,
  k: BigDecimal,
  d: BigDecimal,
  j: BigDecimal,
)

trait KdjMixin(arg1: Int=9, arg2: Int = 3, arg3: Int= 3) extends AStrategy {
  val kdj: mutable.ListBuffer[Kdj] = mutable.ListBuffer.empty

  abstract override def step(k: Kline): Unit = {
    super.step(k)
    if(klines.length < 10) {
      return
    }
    val low9 = klines.slice(0,9).map(_.low).min
    val high9 = klines.slice(0,9).map(_.high).max
    val rsv = (klines(0).close - low9 ) / (high9 - low9) * 100

    val newKdj = if(kdj.isEmpty) {
      val newK = 50 * 2 / 3 + rsv / 3
      val newD = 50 * 2 / 3 + newK / 3
      val newJ = 3 * newK - 2 * newD
      Kdj(k, rsv, newK,newD,newJ)
    }else{
      val preKdj = kdj(0)
      val newK = preKdj.k * 2 / 3 +  rsv / 3
      val newD = preKdj.d * 2 / 3 + newK / 3
      val newJ = 3 * newK - 2 * newD
      Kdj(k, rsv, newK,newD,newJ)
    }
    kdj.prepend(newKdj)
  }
}