---
name: general
version: 1.0.0
description: Expert-level Scala, functional programming and reactive systems
category: languages
tags: [scala, functional-programming, cats, zio]
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash(scala:*, sbt:*)
---

# Scala Expert

Expert guidance for Scala development, functional programming and reactive systems.

## Core Concepts

### Scala Fundamentals
- Immutability
- Pattern matching
- Case classes
- Traits and mixins
- Implicit conversions
- For comprehensions

### Functional Programming
- Higher-order functions
- Monads (Option, Either, Try)
- Functors and Applicatives
- Type classes
- Pure functions
- Referential transparency

### Reactive Systems
- Cats Effect
- ZIO

## Scala Basics

```scala
// Case classes
case class User(id: String, name: String, email: String, age: Int)

// Pattern matching
def processUser(user: User): String = user match {
  case User(_, name, _, age) if age < 18 => s"$name is a minor"
  case User(_, name, _, age) if age >= 65 => s"$name is a senior"
  case User(_, name, _, _) => s"$name is an adult"
}

// Options instead of null
def findUser(id: String): Option[User] = {
  database.get(id)
}

val userName = findUser("123") match {
  case Some(user) => user.name
  case None => "Unknown"
}

// Or using map
val name = findUser("123").map(_.name).getOrElse("Unknown")

// For comprehensions
def getUserWithPosts(userId: String): Option[(User, List[Post])] = {
  for {
    user <- findUser(userId)
    posts <- findPosts(userId)
  } yield (user, posts)
}

// Traits and mixins
trait Serializable {
  def toJson: String
}

trait Loggable {
  def log(message: String): Unit = println(s"[LOG] $message")
}

case class Person(name: String, age: Int) extends Serializable with Loggable {
  def toJson: String = s"""{"name":"$name","age":$age}"""
}

// Implicit classes (extension methods)
implicit class StringOps(s: String) {
  def isValidEmail: Boolean = s.contains("@") && s.contains(".")
}

"test@example.com".isValidEmail // true
```

## Functional Programming

```scala
import cats._
import cats.implicits._

// Functor
val numbers = List(1, 2, 3, 4, 5)
val doubled = numbers.map(_ * 2)

// Applicative
val result = (Option(1), Option(2), Option(3)).mapN { (a, b, c) =>
  a + b + c
}

// Monad (flatMap)
def fetchUser(id: String): Future[Option[User]] = ???
def fetchPosts(userId: String): Future[List[Post]] = ???

val userWithPosts: Future[Option[(User, List[Post])]] = {
  fetchUser("123").flatMap {
    case Some(user) =>
      fetchPosts(user.id).map(posts => Some((user, posts)))
    case None =>
      Future.successful(None)
  }
}

// Or with for-comprehension
val result: Future[Option[(User, List[Post])]] = for {
  userOpt <- fetchUser("123")
  posts <- fetchPosts(userOpt.map(_.id).getOrElse(""))
} yield userOpt.map(user => (user, posts))

// Either for error handling
sealed trait Error
case class NotFound(id: String) extends Error
case class ValidationError(message: String) extends Error

def validateUser(user: User): Either[Error, User] = {
  if (user.email.isValidEmail) Right(user)
  else Left(ValidationError("Invalid email"))
}

def saveUser(user: User): Either[Error, User] = {
  for {
    validated <- validateUser(user)
    saved <- database.save(validated)
  } yield saved
}

// Type classes
trait Show[A] {
  def show(a: A): String
}

object Show {
  def apply[A](implicit sh: Show[A]): Show[A] = sh

  implicit val stringShow: Show[String] = new Show[String] {
    def show(s: String): String = s
  }

  implicit val intShow: Show[Int] = new Show[Int] {
    def show(i: Int): String = i.toString
  }

  implicit def listShow[A: Show]: Show[List[A]] = new Show[List[A]] {
    def show(list: List[A]): String = {
      list.map(Show[A].show).mkString("[", ", ", "]")
    }
  }
}

def print[A: Show](a: A): Unit = {
  println(Show[A].show(a))
}
```

## Cats Effect / ZIO

```scala

// ZIO
import zio._

def fetchUserZIO(id: String): Task[User] = ZIO.attempt {
  database.get(id)
}

val zioProgram: Task[Unit] = for {
  user <- fetchUserZIO("123")
  _ <- Console.printLine(s"Found user: ${user.name}")
} yield ()
```

## Best Practices

### Functional Programming
- Prefer immutability
- Use pure functions
- Avoid side effects
- Use Option/Either over null/exceptions
- Leverage type classes
- Use for-comprehensions
- Apply functional composition

### Scala Style
- Follow naming conventions
- Use case classes for data
- Prefer vals over vars
- Use pattern matching
- Avoid null
- Use implicits carefully
- Write idiomatic code

### Performance
- Use lazy evaluation
- Stream large datasets
- Avoid unnecessary allocations
- Use tail recursion
- Profile before optimizing
- Consider parallelism

## Anti-Patterns

❌ Using null
❌ Mutable state everywhere
❌ God objects
❌ Excessive implicits
❌ Not handling errors
❌ Blocking operations
❌ Not using type safety

## Resources

- Scala Documentation: https://docs.scala-lang.org/
- Akka Documentation: https://doc.akka.io/
- Cats: https://typelevel.org/cats/
- ZIO: https://zio.dev/
- Functional Programming in Scala (Red Book)
