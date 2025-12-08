package com.unischeduler;

import com.microsoft.z3.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z3-unischeduler v2 max-smt engine
 */
public class TimetableSolver {
    private Context ctx;
    private Optimize optimizer;
    private List<Course> courses;
    private List<Room> rooms;

    public TimetableSolver() {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
        optimizer = ctx.mkOptimize();
        courses = new ArrayList<>();
        rooms = new ArrayList<>();
    }
    
    public void addRoom(Room r) { rooms.add(r); }
    public void dispose() { ctx.close(); }
}
