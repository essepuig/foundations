package answers.function

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object GenericFunctionAnswers {

  ////////////////////
  // Exercise 1: Pair
  ////////////////////

  val names: Pair[String] = Pair("John", "Elisabeth")
  val ages: Pair[Int]     = Pair(32, 46)

  case class Pair[A](first: A, second: A) {
    def swap: Pair[A] =
      Pair(second, first)

    def map[To](update: A => To): Pair[To] =
      Pair(update(first), update(second))

    def zipWith[Other, To](other: Pair[Other])(combine: (A, Other) => To): Pair[To] =
      Pair(combine(first, other.first), combine(second, other.second))

    def map3[A2, A3, To](otherB: Pair[A2], otherC: Pair[A3])(combine: (A, A2, A3) => To): Pair[To] =
      zipWith(otherB)((_, _))
        .zipWith(otherC) { case ((a, b), c) => combine(a, b, c) }
  }

  val secret: Pair[List[Byte]] =
    Pair(
      first = List(103, 110, 105, 109, 109, 97, 114, 103, 111, 114, 80),
      second = List(108, 97, 110, 111, 105, 116, 99, 110, 117, 70)
    )
  val decoded: Pair[String] =
    secret
      .map(_.toArray)
      .map(new String(_))
      .map(_.reverse)
      .swap

  case class Product(name: String, price: Double)
  val productNames: Pair[String]  = Pair("Coffee", "Plane ticket")
  val productPrices: Pair[Double] = Pair(2.5, 329.99)
  val products: Pair[Product]     = productNames.zipWith(productPrices)(Product)

  ////////////////////////////
  // Exercise 2: Predicate
  ////////////////////////////

  case class Predicate[A](eval: A => Boolean) {
    def apply(value: A): Boolean = eval(value)

    def &&(other: Predicate[A]): Predicate[A] =
      Predicate(value => eval(value) && other(value))

    def ||(other: Predicate[A]): Predicate[A] =
      Predicate(value => eval(value) || other(value))

    def flip: Predicate[A] =
      Predicate(value => !eval(value))

    def contramap[To](update: To => A): Predicate[To] =
      Predicate[To] { value =>
        val a = update(value)
        eval(a)
      }
  }

  object Predicate {
    def True[A]: Predicate[A]  = Predicate(_ => true)
    def False[A]: Predicate[A] = Predicate(_ => false)
  }

  val isAdult: Predicate[Int] =
    Predicate((age: Int) => age >= 18)

  def isLongerThan(min: Int): Predicate[String] =
    Predicate((text: String) => text.length >= min)

  def contains(char: Char): Predicate[String] =
    Predicate((text: String) => text.contains(char))

  case class User(name: String, age: Int)

  val isValidUser: Predicate[User] =
    isAdult.contramap[User](_.age) &&
      isLongerThan(3).contramap(_.name)

  def biggerThan(min: Int): Predicate[Int] =
    Predicate((x: Int) => x >= min)

  val isAdultV2: Predicate[Int] =
    biggerThan(18)

  def longerThanV2(min: Int): Predicate[String] =
    biggerThan(min).contramap(_.length)

  ////////////////////////////
  // Exercise 3: JsonDecoder
  ////////////////////////////

  // very basic representation of JSON
  type Json = String

  trait JsonDecoder[A] {
    def decode(json: Json): A
  }

  val stringDecoder: JsonDecoder[String] = new JsonDecoder[String] {
    def decode(json: Json): String =
      if (json.startsWith("\"") && json.endsWith("\""))
        json.substring(1, json.length - 1)
      else
        throw new IllegalArgumentException(s"$json is not a JSON string")
  }
  val intDecoder: JsonDecoder[Int] = new JsonDecoder[Int] {
    def decode(json: Json): Int = json.toInt
  }

  // 3a. Implement `userIdDecoder`, a `JsonDecoder` for `UserId`
  // such as userIdDecoder.decoder(UserId("1234")) == 1234
  // Note: Try to re-use `intDecoder` defined below.
  case class UserId(id: Int)
  val userIdDecoder: JsonDecoder[UserId] = new JsonDecoder[UserId] {
    def decode(json: Json): UserId =
      UserId(intDecoder.decode(json))
  }

  // 3b. Implement `localDateDecoder`, a `JsonDecoder` for `LocalDate`
  // such as userIdDecoder.decoder("2020-26-03") == LocalDate.of(2020,26,03)
  // Note: You can parse a `LocalDate` using `LocalDate.parse` with a java.time.format.DateTimeFormatter
  //       Try to re-use `stringDecoder` defined below.
  val localDateDecoder: JsonDecoder[LocalDate] = new JsonDecoder[LocalDate] {
    def decode(json: Json): LocalDate =
      LocalDate.parse(stringDecoder.decode(json), DateTimeFormatter.ISO_LOCAL_DATE)
  }

  // 3c. Implement `map` a generic method that converts a `JsonDecoder`
  // of one type into a `JsonDecoder` of another type.
  def map[From, To](decoder: JsonDecoder[From], update: From => To): JsonDecoder[To] =
    new JsonDecoder[To] {
      def decode(json: Json): To =
        update(decoder.decode(json))
    }

  // 3d. Re-implement a `JsonDecoder` for `UserId and `LocalDate` using `map`
  lazy val userIdDecoderV2: JsonDecoder[UserId] =
    map(intDecoder, (x: Int) => UserId(x))

  lazy val localDateDecoderV2: JsonDecoder[LocalDate] =
    map(stringDecoder, (text: String) => LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE))

  // 3e. (difficult) How would you define and implement a `JsonDecoder` for a generic `Option`?
  // such as we can decode:
  // * "1" into a Some(1)
  // * "2020-26-03" into a Some(LocalDate.of(2020,26,03))
  // * "null" into "None"
  def optionDecoder[A](decoder: JsonDecoder[A]): JsonDecoder[Option[A]] =
    new JsonDecoder[Option[A]] {
      def decode(json: Json): Option[A] =
        json match {
          case "null" => None
          case other  => Some(decoder.decode(json))
        }
    }
}
