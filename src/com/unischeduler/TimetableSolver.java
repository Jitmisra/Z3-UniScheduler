package com.unischeduler;

import com.microsoft.z3.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z3-unischeduler v2 engine
 * upgraded from a simple sat check to a max-smt optimize context.
 * handles room capacities (hard constraints) and prof preferences (soft constraints).
 */
public class TimetableSolver {

    private Context ctx;
    private Optimize optimizer;
    
    private final int MAX_SLOTS = 20;

    private List<Course> courses;
    private List<Room> rooms;
    private Map<Course, IntExpr> courseSlots;
    private Map<Course, IntExpr> courseRooms;

    public TimetableSolver() {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
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
        IntExpr slot = ctx.mkIntConst(c.id + "_slot");
        IntExpr room = ctx.mkIntConst(c.id + "_room");
        courseSlots.put(c, slot);
        courseRooms.put(c, room);

        optimizer.Add(ctx.mkGe(slot, ctx.mkInt(0)));
        optimizer.Add(ctx.mkLt(slot, ctx.mkInt(MAX_SLOTS)));
        
        BoolExpr validRoomConstraint = ctx.mkFalse();
        for (Room r : rooms) {
            BoolExpr isThisRoom = ctx.mkEq(room, ctx.mkInt(r.id));
            if (c.students <= r.capacity) {
                validRoomConstraint = ctx.mkOr(validRoomConstraint, isThisRoom);
            }
        }
        optimizer.Add(validRoomConstraint);

        if (c.professor.preferredSlots != null && c.professor.preferredSlots.length > 0) {
            BoolExpr prefMatch = ctx.mkFalse();
            for (int pSlot : c.professor.preferredSlots) {
                prefMatch = ctx.mkOr(prefMatch, ctx.mkEq(slot, ctx.mkInt(pSlot)));
            }
            optimizer.AssertSoft(prefMatch, 10, "prof_prefs");
        }

        BoolExpr isEarly = ctx.mkLt(slot, ctx.mkInt(15));
        optimizer.AssertSoft(isEarly, 1, "early_classes");
    }

    public void solve() {
        for (int i = 0; i < courses.size(); i++) {
            for (int j = i + 1; j < courses.size(); j++) {
                Course c1 = courses.get(i);
                Course c2 = courses.get(j);
                IntExpr slot1 = courseSlots.get(c1);
                IntExpr slot2 = courseSlots.get(c2);
                IntExpr room1 = courseRooms.get(c1);
                IntExpr room2 = courseRooms.get(c2);
                
                BoolExpr sameSlot = ctx.mkEq(slot1, slot2);
                BoolExpr diffRoom = ctx.mkNot(ctx.mkEq(room1, room2));
                optimizer.Add(ctx.mkImplies(sameSlot, diffRoom));
                
                if (c1.professor.equals(c2.professor)) {
                    optimizer.Add(ctx.mkNot(sameSlot));
                }
            }
        }

        System.out.println("optimizing schedule...");
        Status status = optimizer.Check();

        if (status == Status.SATISFIABLE) {
            System.out.println("success!");
        } else {
            System.out.println("failed: unsat.");
        }
    }
    
    public void dispose() {
        ctx.close();
    }
}
