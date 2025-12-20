package com.unischeduler;

/**
 * main entry point for the z3 unischeduler (v2 max-smt version).
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Z3-UniScheduler Engine [Max-SMT Optimization]");
        System.out.println("Building constraints...");
        
        TimetableSolver solver = new TimetableSolver();
        
        // 1. setup rooms (hard constraints: capacity)
        solver.addRoom(new Room(101, "Turing Hall", 150));
        solver.addRoom(new Room(102, "Lovelace Lab", 40));
        solver.addRoom(new Room(103, "Dijkstra Theatre", 300));
        
        // 2. setup professors (soft constraints: preferences)
        // dr. smith really wants early morning classes (slots 0, 1)
        Professor profSmith = new Professor("Dr. Smith", 0, 1);
        // dr. jones prefers late classes, like slot 10 or 11
        Professor profJones = new Professor("Dr. Jones", 10, 11);
        // dr. brown doesn't care
        Professor profBrown = new Professor("Dr. Brown");

        // 3. setup courses (hard constraints: prof availability, room limits)
        // cs101 is huge, must go in dijkstra theatre
        solver.addCourse(new Course("CS101", "Intro to Java", profSmith, 250));
        solver.addCourse(new Course("CS102", "Data Structs", profSmith, 120)); // smith again!
        
        // cs201 is small, can go anywhere, but jones wants it late
        solver.addCourse(new Course("CS201", "Algorithms", profJones, 35));
        solver.addCourse(new Course("CS301", "OS Design", profJones, 50)); 
        
        // advanced classes
        solver.addCourse(new Course("CS401", "Compilers", profBrown, 25));
        solver.addCourse(new Course("CS402", "Formal Methods", profBrown, 140)); // needs turing or dijkstra
        
        // run the solver
        solver.solve();
        
        solver.dispose();
    }
}
