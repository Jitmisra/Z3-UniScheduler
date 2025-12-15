package com.unischeduler;

import com.microsoft.z3.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z3-unischeduler v2 engine
 * 
 * upgraded from a simple sat check to a max-smt optimize context.
 * handles room capacities (hard constraints) and prof preferences (soft constraints).
 */
public class TimetableSolver {

    private Context ctx;
    private Optimize optimizer;
    
    private final int MAX_SLOTS = 20; // e.g. 5 days * 4 slots

    private List<Course> courses;
    private List<Room> rooms;

    // track the z3 variables for each course (which slot? which room?)
    private Map<Course, IntExpr> courseSlots;
    private Map<Course, IntExpr> courseRooms;

    public TimetableSolver() {
        // setup z3 context
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
        
        // v2 update: we use optimize (max-smt) instead of a simple solver
        optimizer = ctx.mkOptimize();
        
        courses = new ArrayList<>();
        rooms = new ArrayList<>();
        courseSlots = new HashMap<>();
        courseRooms = new HashMap<>();
    }

    public void addRoom(Room r) {
        rooms.add(r);
    }

    public void addCourse(Course c) {
        courses.add(c);
        
        // create variables for the slot and room assigned to this course
        IntExpr slot = ctx.mkIntConst(c.id + "_slot");
        IntExpr room = ctx.mkIntConst(c.id + "_room");
        
        courseSlots.put(c, slot);
        courseRooms.put(c, room);

        // --- HARD CONSTRAINTS ---
        
        // 1. slot must be valid (0 to MAX_SLOTS-1)
        optimizer.Add(ctx.mkGe(slot, ctx.mkInt(0)));
        optimizer.Add(ctx.mkLt(slot, ctx.mkInt(MAX_SLOTS)));
        
        // 2. room must be a valid room id (from the rooms we added)
        BoolExpr validRoomConstraint = ctx.mkFalse();
        for (Room r : rooms) {
            // either it's this room AND the room is big enough...
            BoolExpr isThisRoom = ctx.mkEq(room, ctx.mkInt(r.id));
            
            // if it's this room, it MUST have enough capacity for the students
            if (c.students <= r.capacity) {
                validRoomConstraint = ctx.mkOr(validRoomConstraint, isThisRoom);
            }
        }
        // course must be assigned to ONE of the valid, large-enough rooms
        optimizer.Add(validRoomConstraint);

        // --- SOFT CONSTRAINTS (max-smt optimization) ---
        
        // 3. try really hard to give the professor one of their preferred slots (weight: 10)
        if (c.professor.preferredSlots != null && c.professor.preferredSlots.length > 0) {
            BoolExpr prefMatch = ctx.mkFalse();
            for (int pSlot : c.professor.preferredSlots) {
                prefMatch = ctx.mkOr(prefMatch, ctx.mkEq(slot, ctx.mkInt(pSlot)));
            }
            // add a soft constraint (maximize this)
            optimizer.AssertSoft(prefMatch, 10, "prof_prefs");
        }

        // 4. try to avoid late classes (e.g. slots >= 15 are late afternoon) (penalty: -1)
        BoolExpr isEarly = ctx.mkLt(slot, ctx.mkInt(15));
        optimizer.AssertSoft(isEarly, 1, "early_classes");
    }

    public void solve() {
        // --- RELATIONAL HARD CONSTRAINTS ---
        
        for (int i = 0; i < courses.size(); i++) {
            for (int j = i + 1; j < courses.size(); j++) {
                Course c1 = courses.get(i);
                Course c2 = courses.get(j);
                
                IntExpr slot1 = courseSlots.get(c1);
                IntExpr slot2 = courseSlots.get(c2);
                IntExpr room1 = courseRooms.get(c1);
                IntExpr room2 = courseRooms.get(c2);
                
                // constraint: if they are in the exact same slot, they CANNOT be in the same room
                BoolExpr sameSlot = ctx.mkEq(slot1, slot2);
                BoolExpr diffRoom = ctx.mkNot(ctx.mkEq(room1, room2));
                optimizer.Add(ctx.mkImplies(sameSlot, diffRoom));
                
                // constraint: a professor cannot teach two things at once
                if (c1.professor.equals(c2.professor)) {
                    optimizer.Add(ctx.mkNot(sameSlot));
                }
            }
        }

        System.out.println("Z3 Max-SMT: Optimizing schedule for " + courses.size() + " courses across " + rooms.size() + " rooms...");
        Status status = optimizer.Check();

        if (status == Status.SATISFIABLE) {
            System.out.println("\nSUCCESS: Optimal Schedule Found!\n");
            Model model = optimizer.getModel();
            printGrid(model);
        } else {
            System.out.println("\nFAILED: The constraints are too tight. UNSAT.");
            // in a real app we'd extract the unsat core here to figure out why!
        }
    }

    private void printGrid(Model model) {
        // group by slot to make a nice timetable
        Map<Integer, List<Course>> schedule = new HashMap<>();
        
        for (Course c : courses) {
            int assignedSlot = Integer.parseInt(model.evaluate(courseSlots.get(c), false).toString());
            schedule.putIfAbsent(assignedSlot, new ArrayList<>());
            schedule.get(assignedSlot).add(c);
        }

        for (int s = 0; s < MAX_SLOTS; s++) {
            if (schedule.containsKey(s)) {
                System.out.println("Slot " + String.format("%02d", s) + ":");
                for (Course c : schedule.get(s)) {
                    int assignedRoomId = Integer.parseInt(model.evaluate(courseRooms.get(c), false).toString());
                    String rName = getRoomName(assignedRoomId);
                    
                    boolean wasPreferred = false;
                    for (int pref : c.professor.preferredSlots) {
                        if (pref == s) wasPreferred = true;
                    }
                    
                    String star = wasPreferred ? " [★ Preferred]" : "";
                    
                    System.out.printf("  %-10s | %-12s | %-16s | %-10s (Cap: %d, Enrolled: %d)%s\n", 
                        c.id, c.name, c.professor.name, rName, getRoomCap(assignedRoomId), c.students, star);
                }
                System.out.println("---------------------------------------------------------------------------------------------------");
            }
        }
    }

    private String getRoomName(int id) {
        for (Room r : rooms) if (r.id == id) return r.name;
        return "Unknown";
    }

    private int getRoomCap(int id) {
        for (Room r : rooms) if (r.id == id) return r.capacity;
        return 0;
    }

    public void dispose() {
        ctx.close();
    }
}
