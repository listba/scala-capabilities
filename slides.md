---
marp: true
theme: gaia
size: "16:9"
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
var x = 6

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



---
## Pure Functions


---
## 

```scala
```

--- 
## Transativity

---
## Separation Checking



---
## Why is this useful?

- AI
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

## Videos
-

</div>



