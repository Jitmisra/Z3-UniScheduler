package com.unischeduler;

import com.microsoft.z3.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z3-unischeduler v1 simple solver
 */
public class TimetableSolver {
    private Context ctx;
    private Solver solver;
    private List<Course> courses;
    private List<Room> rooms;

    public TimetableSolver() {
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        ctx = new Context(cfg);
        solver = ctx.mkSolver();
        courses = new ArrayList<>();
        rooms = new ArrayList<>();
    }
    
    public void dispose() {
        ctx.close();
    }
}
