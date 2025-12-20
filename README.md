# Z3-UniScheduler (University Timetabling Constraint Solver)

This was a side project I started to solve the recurring nightmare of our department's manual course scheduling. It's built on top of the **Z3 SMT Solver** (using the Java API). 

The goal was to take a list of professors, their preferred slots, room capacities, and core course requirements, then output a conflict-free schedule. If no such schedule exists, it should (ideally) suggest the "least bad" alternative.

## Why Z3?
I tried a basic backtracking search in Python first, but it just wasn't scaling well once I added room capacity constraints. Z3's bit-vector and integer theories are perfect for this—you just define the rules (constraints) and let the solver do the heavy lifting. I specifically moved to Z3's `Optimize` (Max-SMT) context because setting hard constraints just wasn't enough; we needed soft constraints to optimize professor happiness (preferred slots) and penalize late-night classes.

## Current status (V2.0 Engine)
- [x] Basic prof/slot conflict resolution
- [x] Room capacity checks (Hard Constraint)
- [x] Max-SMT optimization for professor preferences (Soft Constraint)
- [x] SMT-LIB2 string output for debugging
- [x] ASCII schedule grid output
- [ ] Priority-based "soft" constraints for lunch breaks

## How to build/run
You'll need the `z3` binary and the `com.microsoft.z3.jar` in the `lib/` folder.

```bash
javac -cp "lib/com.microsoft.z3.jar" src/com/unischeduler/*.java -d bin/
java -cp "bin/:lib/com.microsoft.z3.jar" com.unischeduler.Main
```

*Note: This is still a work in progress from my sophomore year.*
