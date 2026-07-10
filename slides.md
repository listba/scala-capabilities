---
marp: true
theme: gaia
style: |
  .columns {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }

---
<!-- _class: lead -->
# Safer Scala 
## Capture Checking & Capabilities

---
## What is a capture?


```scala
val x = 6

def compute(y: Int): Int = 
    x * y;
```
In the code below we can say that `compute` captures `x`
ie: compute holds on to a copy or reference to `x` even if called from a scope where `x` is unavailable
This isn't inhernetly bad

<!-- Blah Blah Blah -->

---
## Leaked resource

```scala
def withFile[T](name: String)(op: InputStream => T): T =
  Using.resource(new FileInputStream(name)) { f =>
    op(f)
  }

val f = withFile("data.txt") { identity }
f.available() // boom
```
```sh
src/example-01/$ scala run withFile.sc
Exception in thread "main" java.io.IOException: Stream Closed ...
```
This compiles, but blows up with a runtime exception when the leaked file reference is used

--- 
## Compiler error for leaked captures

```scala
def withFile[T](name: String)(op: InputStream^ => T): T =
  Using.resource(new FileInputStream(name)) { f =>
    op(f)
  }

val f = withFile("data.txt") { identity }
f.available()
```
```sh
src/example-01/$ scala run withFileCaptured.sc
[error] Capability `x` outlives its scope: it leaks into outer capture set 's1 which is owned by value f.
[error] The leakage occurred when trying to match the following types:
[error] 
[error] Found:    (x: java.io.InputStream^) ->'s2 java.io.InputStream^{x}
[error] Required: java.io.InputStream^ => java.io.InputStream^'s1
[error] 
[error] where:    => refers to a root capability created in value f when checking argument to parameter op of method withFile
[error]           ^  refers to the root capability caps.any
[error] val f = withFile("data.txt") { identity }
[error]                                ^^^^^^^^
```

---
## Tracking Captures as types

type `T` captures capabilities `c_1` ... `c_n`
```scala
T^{c_1, ..., c_n}
```
eg: a function from `A` to `B` that captures value `c`
```scala
(A -> B)^{c}
// or
A ->{c} B
```

Note that a standalone `^` eg `T^` is equivilant to `T^{any}`
and that `T` is equivilant to `T^{}` ie an empty capture set
capture subtyping will be covered  a bit later

---
## Capabilities

A capability is semantically a "value of interest". Typically something considered to be "effectual"
Capabilities are considered "tracked"
Therefore marking something with `^` means it is a capability
Anytime something marked as a capability is leaked outside of it's scope results in a compilation error


--- 
## Transativity

Building off our example from before
```scala
trait Foo{ val x: Int }
def mkFoo(in: InputStream^): Foo = new Foo { val x = 42 }

withFile("data.txt"): in =>
  val foo = mkFoo(in)
  foo.x
```

This compiles, and returns `42`
`def mkFoo(in: InputStream^): Foo`
Here we are making a `guarantee` that `Foo` does not capture the `in` capability

---
## Transativity (cont)
However, if instead we had

```scala
class SneakyFooImpl(val x: Int, in: InputStream) extends Foo
def mkFoo(in: InputStream^): Foo = SneakyFooImpl(42, in)
```
the compiler is going to complain due to `hiding`
```sh
src/example-02$ scala run transitiveBad.sc
[error] Note that capability `in²` cannot flow into capture set {}.
[error] 
[error] where:    in  is a value in class SneakyFooImpl
[error]           in² is a parameter in method mkFoo
[error] def mkFoo(in: InputStream^): Foo = SneakyFooImpl(42, in)
```

---
## Transativity (cont)
This can be fixed by updating mkFoo to report that we are capturing some capability
```scala
def mkFoo(in: InputStream^): Foo^ = SneakyFooImpl(42, in)
```
However, we can also refine this a bit, by telling the compiler that we might be capturing `in` 
or worded slightly differntly we are telling the compiler that mkFoo at most is capturing `in`
```scala
def mkFoo(in: InputStream^): Foo^{in} = SneakyFooImpl(42, in)
```

---
## Capture subtyping

Based on the previous slides we can start to put together a picture of how subtyping works in our example

```scala
Foo <: Foo^{in} <: Foo^
```

Which can be generalized to 

```scala
T = T^{} <: T^{c1} <: T^{c1,c2} = T^{c2,c1}  <: T^ = T^{any}
```

this means captures can be `widened` to include other known or unknown captures in the group

---
## Pure and impure Functions

`->` is now a new symbol in scala that means we have a `pure` function
eg: `A -> B` is equivilant to `A -> {} B`

where as `=>` is defined as an `impure` function that captues and unknown set of capabilities 
eg `A => B` is equivilant to `A => {any} B`

and  finally `A -> {c1} -> B` is a function that tracks `c1`

thus we have
```scala
(A -> B) <: (A ->{c1} -> B) <: (A ->{any} B) = A => B
```


---



---
## Why is this useful?

- Stricter compiler checks gives us better confidence in AI Generated code
- AI "Agent" harnesses built using "capabilities" as permission structure for safer ai generation
    - See paper at end of slides

[Anthropic bun compiler Rust rewrite](https://bun.com/blog/bun-in-rust)
<!-- 
TODO: Find any sources on Uptick in rust usage due to AI
-->
---
## Where is it going
- [Gears](https://github.com/lampepfl/gears) - a concurrency library utilizing capabilities and captures
- [Ox](https://github.com/softwaremill/ox) - Another concurance library in scala
- [Cats MTL](https://github.com/typelevel/cats-mtl) - Algebraic Effect library for cats-effect
    - [Errors as Capabilities](https://typelevel.org/blog/custom-error-types.html)
- [OCaml OxCaml](https://github.com/oxcaml/oxcaml)

---
## Theres way more

- [Checked Exceptions](https://docs.scala-lang.org/scala3/reference/experimental/capture-checking/checked-exceptions.html)
- [Stateful Capabilities](https://docs.scala-lang.org/scala3/reference/experimental/capture-checking/mutability.html)
- [Seperation Checking](https://docs.scala-lang.org/scala3/reference/experimental/capture-checking/separation-checking.html)

---
# Resources

<div class="columns">
<div>

## Documentation
- [Capture Checking](https://docs.scala-lang.org/scala3/reference/experimental/capture-checking/index.html) 
- [Pure Functions](https://docs.scala-lang.org/scala3/reference/experimental/purefuns.html)
- [CanThrow Capabilities](https://docs.scala-lang.org/scala3/reference/experimental/canthrow.html)

</div>
<div>

## Papers
- [Capturing Types](https://dl.acm.org/doi/10.1145/3618003)
- [Capture Tracking over Generic Data Structures](https://dl.acm.org/doi/abs/10.1145/3763112)
- [Tracking Capabilities for Safer Agents](https://arxiv.org/pdf/2603.00991)

</div>
</div>
<div>


</div>



